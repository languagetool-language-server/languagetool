/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2018, MPL Adam Voss vossad01@gmail.com
 *
 * In memory of Adam Voss, original creator
 * July 11, 1991 - July 11, 2018
 * https://github.com/adamvoss
 * http://schluterbalikfuneralhome.com/obituary/adam-voss
 *
 */
package org.languagetool.languageserver;

// import com.vladsch.flexmark.ast.Document;
// import com.vladsch.flexmark.parser.Parser;
// import org.languagetool.languageserver.markdown.AnnotatedTextBuildingVisitor;
import org.eclipse.lsp4j.*;
import org.eclipse.lsp4j.services.*;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.languagetool.JLanguageTool;
import org.languagetool.Language;
import org.languagetool.Languages;
import org.languagetool.markup.AnnotatedText;
import org.languagetool.markup.AnnotatedTextBuilder;
import org.languagetool.rules.RuleMatch;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.Stream;

class LanguageToolLanguageServer implements LanguageServer, LanguageClientAware {

  HashMap<String, TextDocumentItem> documents = new HashMap<String, TextDocumentItem>();
  private LanguageClient client = null;
  @Nullable
  private Language language;

  private static boolean locationOverlaps(RuleMatch match, DocumentPositionCalculator positionCalculator, Range range) {
    return overlaps(range, createDiagnostic(match, positionCalculator).getRange());
  }

  private static boolean overlaps(Range r1, Range r2) {
    return r1.getStart().getCharacter() <= r2.getEnd().getCharacter()
        && r1.getEnd().getCharacter() >= r2.getStart().getCharacter()
        && r1.getStart().getLine() >= r2.getEnd().getLine() && r1.getEnd().getLine() <= r2.getStart().getLine();
  }

  private static Diagnostic createDiagnostic(RuleMatch match, DocumentPositionCalculator positionCalculator) {
    Diagnostic ret = new Diagnostic();
    ret.setRange(new Range(positionCalculator.getPosition(match.getFromPos()),
        positionCalculator.getPosition(match.getToPos())));
    ret.setSeverity(DiagnosticSeverity.Warning);
    ret.setSource(String.format("LanguageTool: %s", match.getRule().getDescription()));
    ret.setMessage(match.getMessage());
    return ret;
  }

  @Override
  public CompletableFuture<InitializeResult> initialize(InitializeParams params) {
    ServerCapabilities capabilities = new ServerCapabilities();
    capabilities.setTextDocumentSync(TextDocumentSyncKind.Full);
    capabilities.setCodeActionProvider(true);
    capabilities
        .setExecuteCommandProvider(new ExecuteCommandOptions(Collections.singletonList(TextEditCommand.CommandName)));
    return CompletableFuture.completedFuture(new InitializeResult(capabilities));
  }

  @Override
  public CompletableFuture<Object> shutdown() {
    // Per https://github.com/eclipse/lsp4j/issues/18
    return CompletableFuture.completedFuture(new Object());
  }

  @Override
  public void exit() {
  }

  @Override
  public TextDocumentService getTextDocumentService() {
    return new FullTextDocumentService(documents) {

      @Override
      public CompletableFuture<List<Either<Command, CodeAction>>> codeAction(CodeActionParams params) {
        if (params.getContext().getDiagnostics().isEmpty()) {
          return CompletableFuture.completedFuture(Collections.emptyList());
        }

        TextDocumentItem document = documents.get(params.getTextDocument().getUri());

        List<RuleMatch> matches = validateDocument(document);

        DocumentPositionCalculator positionCalculator = new DocumentPositionCalculator(document.getText());

        Stream<RuleMatch> relevant = matches.stream()
            .filter(m -> locationOverlaps(m, positionCalculator, params.getRange()));

        List<Either<Command, CodeAction>> commands = relevant
            .flatMap(m -> getEditCommands(m, document, positionCalculator)).map(Either::<Command, CodeAction>forLeft)
            .collect(Collectors.toList());

        return CompletableFuture.completedFuture(commands);
      }

      @NotNull
      private Stream<TextEditCommand> getEditCommands(RuleMatch match, TextDocumentItem document,
          DocumentPositionCalculator positionCalculator) {
        Range range = createDiagnostic(match, positionCalculator).getRange();
        return match.getSuggestedReplacements().stream().map(str -> new TextEditCommand(str, range, document));
      }

      @Override
      public void didOpen(DidOpenTextDocumentParams params) {
        super.didOpen(params);

        publishIssues(params.getTextDocument().getUri());
      }

      @Override
      public void didChange(DidChangeTextDocumentParams params) {
        super.didChange(params);

        publishIssues(params.getTextDocument().getUri());
      }

      private void publishIssues(String uri) {
        TextDocumentItem document = this.documents.get(uri);
        LanguageToolLanguageServer.this.publishIssues(document);
      }
    };
  }

  private void publishIssues(TextDocumentItem document) {
    List<Diagnostic> diagnostics = getIssues(document);

    client.publishDiagnostics(new PublishDiagnosticsParams(document.getUri(), diagnostics));
  }

  private List<Diagnostic> getIssues(TextDocumentItem document) {
    List<RuleMatch> matches = validateDocument(document);

    DocumentPositionCalculator positionCalculator = new DocumentPositionCalculator(document.getText());

    return matches.stream().map(match -> createDiagnostic(match, positionCalculator)).collect(Collectors.toList());
  }

  private List<RuleMatch> validateDocument(TextDocumentItem document) {
    // This setting is specific to VS Code behavior and maintaining it here
    // long term is not desirable because other clients may behave differently.
    // See: https://github.com/Microsoft/vscode/issues/28732
    String uri = document.getUri();
    Boolean isSupportedScheme = uri.startsWith("file:") || uri.startsWith("untitled:");

    if (language == null || !isSupportedScheme) {
      return Collections.emptyList();
    } else {
      JLanguageTool languageTool = new JLanguageTool(language);

      String languageId = document.getLanguageId();
      
      try {
        switch (languageId) {
          case "text": {
            AnnotatedText aText;
            aText = new AnnotatedTextBuilder().addText(document.getText()).build();
            return languageTool.check(aText);
          }
          case "annotatedtext": {
            AnnotatedText aText;
            ObjectMapper mapper = new ObjectMapper();
            JsonNode data = mapper.readTree(document.getText());
            aText = getAnnotatedTextFromJson(data);
            return languageTool.check(aText);
          }
          default: {
            throw new UnsupportedOperationException(String.format("Language, %s, is not supported.", languageId));
          }
        }
      } catch (IOException e) {
        e.printStackTrace();
        return Collections.emptyList();
      }
    }
  }

  @Override
  public WorkspaceService getWorkspaceService() {
    return new NoOpWorkspaceService() {
      @Override
      public void didChangeConfiguration(DidChangeConfigurationParams params) {
        super.didChangeConfiguration(params);

        setLanguage(params.getSettings());
      }

      @SuppressWarnings({"unchecked", "rawtypes"})
      @Override
      public CompletableFuture<Object> executeCommand(ExecuteCommandParams params) {
        if (Objects.equals(params.getCommand(), TextEditCommand.CommandName)) {
          return ((CompletableFuture<Object>) (CompletableFuture) client
              .applyEdit(new ApplyWorkspaceEditParams(new WorkspaceEdit(
                  (List<Either<TextDocumentEdit, ResourceOperation>>) (List) params.getArguments()))));
        }
        return CompletableFuture.completedFuture(false);
      }
    };
  }

  @SuppressWarnings("unchecked")
  private void setLanguage(@NotNull Object settingsObject) {
    Map<String, Object> settings = (Map<String, Object>) settingsObject;
    Map<String, Object> languageServerExample = (Map<String, Object>) settings.get("languageTool");
    String shortCode = ((String) languageServerExample.get("language"));

    setLanguage(shortCode);
  }

  private void setLanguage(String shortCode) {
    if (Languages.isLanguageSupported(shortCode)) {
      language = Languages.getLanguageForShortCode(shortCode);
    } else {
      System.out.println("ERROR: " + shortCode + " is not a recognized language.  Checking disabled.");
      language = null;
    }

    documents.values().forEach(this::publishIssues);
  }

  @Override
  public void connect(LanguageClient client) {
    this.client = client;
  }

  // From org.languagetool.server.ApiV2
  private AnnotatedText getAnnotatedTextFromString(JsonNode data, String text) {
    AnnotatedTextBuilder textBuilder = new AnnotatedTextBuilder().addText(text);
    if (data.has("metaData")) {
      JsonNode metaData = data.get("metaData");
      Iterator<String> it = metaData.fieldNames();
      while (it.hasNext()) {
        String key = it.next();
        String val = metaData.get(key).asText();
        try {
          AnnotatedText.MetaDataKey metaDataKey = AnnotatedText.MetaDataKey.valueOf(key);
          textBuilder.addGlobalMetaData(metaDataKey, val);
        } catch (IllegalArgumentException e) {
          textBuilder.addGlobalMetaData(key, val);
        }
      }
    }
    return textBuilder.build();
  }

  // From org.languagetool.server.ApiV2
  private AnnotatedText getAnnotatedTextFromJson(JsonNode data) {
    AnnotatedTextBuilder atb = new AnnotatedTextBuilder();
    // Expected format:
    // annotation: [
    //   {text: 'text'},
    //   {markup: '<b>'}
    //   {text: 'more text'},
    //   {markup: '</b>'}
    // ]
    //
    for (JsonNode node : data.get("annotation")) {
      if (node.get("text") != null && node.get("markup") != null) {
        throw new RuntimeException("Only either 'text' or 'markup' are supported in an object in 'annotation' list, not both: " + node);
      } else if (node.get("text") != null && node.get("interpretAs") != null) {
        throw new RuntimeException("'text' cannot be used with 'interpretAs' (only 'markup' can): " + node);
      } else if (node.get("text") != null) {
        atb.addText(node.get("text").asText());
      } else if (node.get("markup") != null) {
        if (node.get("interpretAs") != null) {
          atb.addMarkup(node.get("markup").asText(), node.get("interpretAs").asText());
        } else {
          atb.addMarkup(node.get("markup").asText());
        }
      } else {
        throw new RuntimeException("Only 'text' and 'markup' are supported in 'annotation' list: " + node);
      }
    }
    return atb.build();
  }

}

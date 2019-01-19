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

import org.eclipse.lsp4j.Command;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.TextDocumentEdit;
import org.eclipse.lsp4j.TextDocumentItem;
import org.eclipse.lsp4j.VersionedTextDocumentIdentifier;
import org.eclipse.lsp4j.TextEdit;

import java.util.Collections;

class TextEditCommand extends Command {

  private static final String CommandName = "langugageTool.acceptSuggestion";

  public TextEditCommand(String title, Range range, TextDocumentItem document) {
    this.setCommand(CommandName);

    VersionedTextDocumentIdentifier id = new VersionedTextDocumentIdentifier(document.getUri(), document.getVersion());
    id.setUri(document.getUri());
    this.setArguments(
        Collections.singletonList(new TextDocumentEdit(id, Collections.singletonList(new TextEdit(range, title)))));
    this.setTitle(title);
  }

  public static final String getCommandName() {
    return TextEditCommand.CommandName;
  }

}

/* LanguageTool, a natural language style checker 
 * Copyright (C) 2012 Jaume Ortolà i Font
 * 
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301
 * USA
 */
package org.languagetool.rules.ca;

import org.languagetool.AnalyzedSentence;
import org.languagetool.AnalyzedToken;
import org.languagetool.AnalyzedTokenReadings;
import org.languagetool.JLanguageTool;
import org.languagetool.rules.Category;
import org.languagetool.rules.RuleMatch;
import org.languagetool.tools.StringTools;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This rule checks if a word without graphical accent and with a verb POS tag should be
 * a noun or an adjective with graphical accent.  
 * It uses two lists of word pairs: verb-noun and verb-adjective.
 *   
 * @author Jaume Ortolà i Font
 */
public class AccentuationCheckRule extends CatalanRule {

  private static final String FILE_NAME = "/ca/verb_senseaccent_nom_ambaccent.txt";
  private static final String FILE_NAME2 = "/ca/verb_senseaccent_adj_ambaccent.txt";
  private static final String FILE_ENCODING = "utf-8";
  private final Map<String, AnalyzedTokenReadings> relevantWords;
  private final Map<String, AnalyzedTokenReadings> relevantWords2;
  
   /**
   * Patterns
   */
  private static final Pattern PREPOSICIO_DE = Pattern.compile("de|d'|del|dels");
  private static final Pattern ARTICLE_EL_MS = Pattern.compile("el|l'|El|L'");
  private static final Pattern ARTICLE_EL_FS = Pattern.compile("la|l'|La|L'");
  private static final Pattern ARTICLE_EL_MP = Pattern.compile("els|Els");
  private static final Pattern ARTICLE_EL_FP = Pattern.compile("les|Les");
  private static final Pattern DETERMINANT = Pattern.compile("D[^R].*");
  private static final Pattern DETERMINANT_MS = Pattern.compile("D[^R].[MC][SN].*");
  private static final Pattern DETERMINANT_FS = Pattern.compile("D[^R].[FC][SN].*");
  private static final Pattern DETERMINANT_MP = Pattern.compile("D[^R].[MC][PN].*");
  private static final Pattern DETERMINANT_FP = Pattern.compile("D[^R].[FC][PN].*");
  private static final Pattern NOM_MS = Pattern.compile("NC[MC][SN].*");
  private static final Pattern NOM_FS = Pattern.compile("NC[FC][SN].*");
  private static final Pattern NOM_MP = Pattern.compile("NC[MC][PN].*");
  private static final Pattern NOM_FP = Pattern.compile("NC[FC][PN].*");
  private static final Pattern ADJECTIU_MS = Pattern.compile("AQ.[MC][SN].*|V.P..SM");
  private static final Pattern ADJECTIU_FS = Pattern.compile("AQ.[FC][SN].*|V.P..SF");
  private static final Pattern ADJECTIU_MP = Pattern.compile("AQ.[MC][PN].*|V.P..PM");
  private static final Pattern ADJECTIU_FP = Pattern.compile("AQ.[FC][PN].*|V.P..PF");
  private static final Pattern INFINITIU = Pattern.compile("V.N.*");
  private static final Pattern VERB_CONJUGAT = Pattern.compile("V.[^NGP].*");
  private static final Pattern NOT_IN_PREV_TOKEN = Pattern.compile("VA.*|PP.*|P0.*|VSP.*");
  private static final Pattern BEFORE_ADJECTIVE_MS = Pattern.compile("SPS00|D[^R].[MC][SN].*|V.[^NGP].*|PX.*");
  private static final Pattern BEFORE_ADJECTIVE_FS = Pattern.compile("SPS00|D[^R].[FC][SN].*|V.[^NGP].*|PX.*");
  private static final Pattern BEFORE_ADJECTIVE_MP = Pattern.compile("SPS00|D[^R].[MC][PN].*|V.[^NGP].*|PX.*");
  private static final Pattern BEFORE_ADJECTIVE_FP = Pattern.compile("SPS00|D[^R].[FC][PN].*|V.[^NGP].*|PX.*");
      
    
 public AccentuationCheckRule(ResourceBundle messages) throws IOException {
    if (messages != null) {
      super.setCategory(new Category(messages.getString("category_misc")));
    }
    relevantWords = loadWords(JLanguageTool.getDataBroker().getFromRulesDirAsStream(FILE_NAME)); 
    relevantWords2 = loadWords(JLanguageTool.getDataBroker().getFromRulesDirAsStream(FILE_NAME2)); 
  }
  
  @Override
  public String getId() {
    return "ACCENTUATION_CHECK";
  }

  @Override
  public String getDescription() {
    return "Comprova si la paraula ha de dur accent gr\u00E0fic.";
  }

  @Override
  public RuleMatch[] match(final AnalyzedSentence text) {
    final List<RuleMatch> ruleMatches = new ArrayList<RuleMatch>();
    final AnalyzedTokenReadings[] tokens = text.getTokensWithoutWhitespace();
    //ignoring token 0, i.e., SENT_START
    for (int i = 1; i < tokens.length; i++) {
      String token;
      if (i==1) { 
        token=tokens[i].getToken().toLowerCase();
      }
      else {
        token=tokens[i].getToken();
      }
      String prevToken = tokens[i-1].getToken();
      String prevPrevToken="";
      if (i>2) {
        prevPrevToken = tokens[i-2].getToken();
      }
      String nextToken="";
      if (i<tokens.length-1) {
        nextToken = tokens[i+1].getToken();
      }
      String nextNextToken="";
      if (i<tokens.length-2) {
        nextNextToken = tokens[i+2].getToken();
      }
      boolean isRelevantWord = false;
      boolean isRelevantWord2 = false;
      if (StringTools.isEmpty(token)) {          
        continue;
      }
      if (relevantWords.containsKey(token)) {
        isRelevantWord = true;
      }
      if (relevantWords2.containsKey(token)) {
        isRelevantWord2 = true;
      }

      String msg = null;        
      String replacement = null;
      final Matcher mPreposicioDE = PREPOSICIO_DE.matcher(nextToken);
      final Matcher mArticleELMS = ARTICLE_EL_MS.matcher(prevToken);
      final Matcher mArticleELFS = ARTICLE_EL_FS.matcher(prevToken);
      final Matcher mArticleELMP = ARTICLE_EL_MP.matcher(prevToken);
      final Matcher mArticleELFP = ARTICLE_EL_FP.matcher(prevToken);
      
      // verb without accent -> noun with accent   
      if (isRelevantWord)
      { 
      	//amb renuncies    	 
        if (tokens[i-1].hasPosTag("SPS00") && !matchPostagRegexp(tokens[i],INFINITIU) ) 
      	{
      		replacement = relevantWords.get(token).getToken();
      	}
      	//aquestes renuncies
      	else if ( 
      	         ((matchPostagRegexp(tokens[i-1],DETERMINANT_MS) && matchPostagRegexp(relevantWords.get(token),NOM_MS)
      	           && !token.equals("cantar") )
      	        ||(matchPostagRegexp(tokens[i-1],DETERMINANT_MP) && matchPostagRegexp(relevantWords.get(token),NOM_MP)) 
      	        ||(matchPostagRegexp(tokens[i-1],DETERMINANT_FS) && matchPostagRegexp(relevantWords.get(token),NOM_FS) 
      	           && !token.equals("venia") && !token.equals("tenia") && !token.equals("continua") && !token.equals("genera") ) 
      	        ||(matchPostagRegexp(tokens[i-1],DETERMINANT_FP) && matchPostagRegexp(relevantWords.get(token),NOM_FP)) ) ) 
      	{
      		replacement = relevantWords.get(token).getToken();
      	}
      	//circumstancies d'un altre caire
      	else if  ( !token.equals("venia") && !token.equals("venies") && !token.equals("tenia") && !token.equals("tenies") 
      	           && !token.equals("continua") && !token.equals("continues") && !token.equals("cantar") 
      	           && mPreposicioDE.matches() && !matchPostagRegexp(tokens[i-1],NOT_IN_PREV_TOKEN) 
      	           && (i<tokens.length-2) && !matchPostagRegexp(tokens[i+2],INFINITIU) 
      	           && !tokens[i-1].hasPosTag("RG") ) 
      	{
      		replacement = relevantWords.get(token).getToken();
      	}
      	//la renuncia del president. 
      	else if ( !token.equals("venia") && !token.equals("venies") && !token.equals("tenia") && !token.equals("tenies") 
      	           && !token.equals("continua") && !token.equals("continues") && !token.equals("cantar") 
      	           && !token.equals("diferencia") && !token.equals("diferencies") && !token.equals("distancia") && !token.equals("distancies")
      	         &&(  ( mArticleELMS.matches() && matchPostagRegexp(relevantWords.get(token),NOM_MS) )
      	           || ( mArticleELFS.matches() && matchPostagRegexp(relevantWords.get(token),NOM_FS) )
      	           || ( mArticleELMP.matches() && matchPostagRegexp(relevantWords.get(token),NOM_MP) )
      	           || ( mArticleELFP.matches() && matchPostagRegexp(relevantWords.get(token),NOM_FP) ) )      	         
      	         
      	         && mPreposicioDE.matches() 
      	         ) 
      	{
      		replacement = relevantWords.get(token).getToken();
      	}   
      	//circumstancies extraordinàries     
      	else if ( !token.equals("pronuncia") && !token.equals("pronuncies") && !token.equals("venia") && !token.equals("venies") 
      	          && !token.equals("tenia") && !token.equals("tenies") && !token.equals("continua") && !token.equals("continues") 
      	          && (i<tokens.length-1) && 
      	          (
      	            (matchPostagRegexp(relevantWords.get(token),NOM_MS) && matchPostagRegexp(tokens[i+1],ADJECTIU_MS))
      	            || (matchPostagRegexp(relevantWords.get(token),NOM_FS) && matchPostagRegexp(tokens[i+1],ADJECTIU_FS))
      	            || (matchPostagRegexp(relevantWords.get(token),NOM_MP) && matchPostagRegexp(tokens[i+1],ADJECTIU_MP))
      	            || (matchPostagRegexp(relevantWords.get(token),NOM_FP) && matchPostagRegexp(tokens[i+1],ADJECTIU_FP))
      	          )
      	            ) 
      	{
      		replacement = relevantWords.get(token).getToken();
      	}
      	// les circumstancies que ens envolten
      	else if ( nextToken.equals("que") && 
      	           (  ( mArticleELMS.matches() && matchPostagRegexp(relevantWords.get(token),NOM_MS) )
      	           || ( mArticleELFS.matches() && matchPostagRegexp(relevantWords.get(token),NOM_FS) )
      	           || ( mArticleELMP.matches() && matchPostagRegexp(relevantWords.get(token),NOM_MP) )
      	           || ( mArticleELFP.matches() && matchPostagRegexp(relevantWords.get(token),NOM_FP) ) )   
      	         ) 
      	{
      		replacement = relevantWords.get(token).getToken();
      	}
      }
      
      // verb without accent -> adjective with accent
      if (isRelevantWord2)
      { 
      	 // de manera obvia, circumstàncies extraordinaries. 
         if (    (matchPostagRegexp(relevantWords2.get(token),ADJECTIU_MS) && matchPostagRegexp(tokens[i-1],NOM_MS) && !tokens[i-1].hasPosTag("_GN_FS") && matchPostagRegexp(tokens[i],VERB_CONJUGAT) ) 
      	     || (matchPostagRegexp(relevantWords2.get(token),ADJECTIU_FS) && prevPrevToken.equalsIgnoreCase("de") && (prevToken.equals("manera")||prevToken.equals("forma")) )
      	     || (matchPostagRegexp(relevantWords2.get(token),ADJECTIU_MP) && matchPostagRegexp(tokens[i-1],NOM_MP))
      	     || (matchPostagRegexp(relevantWords2.get(token),ADJECTIU_FP) && matchPostagRegexp(tokens[i-1],NOM_FP))
      	    )
      	 {
      	 	  replacement = relevantWords2.get(token).getToken();
      	 }
      	 // de continua disputa
      	 else if ( (i<tokens.length-1) && !prevToken.equals("que") && !matchPostagRegexp(tokens[i-1],NOT_IN_PREV_TOKEN)  &&  	 
      	      ( (matchPostagRegexp(relevantWords2.get(token),ADJECTIU_MS) && matchPostagRegexp(tokens[i+1],NOM_MS) && matchPostagRegexp(tokens[i-1],BEFORE_ADJECTIVE_MS) ) 
      	     || (matchPostagRegexp(relevantWords2.get(token),ADJECTIU_FS) && matchPostagRegexp(tokens[i+1],NOM_FS) && matchPostagRegexp(tokens[i-1],BEFORE_ADJECTIVE_FS) )
      	     || (matchPostagRegexp(relevantWords2.get(token),ADJECTIU_MP) && matchPostagRegexp(tokens[i+1],NOM_MP) && matchPostagRegexp(tokens[i-1],BEFORE_ADJECTIVE_MP) )
      	     || (matchPostagRegexp(relevantWords2.get(token),ADJECTIU_FP) && matchPostagRegexp(tokens[i+1],NOM_FP) && matchPostagRegexp(tokens[i-1],BEFORE_ADJECTIVE_FP) ) )
      	    )
      	 {
      	 	  replacement = relevantWords2.get(token).getToken();
      	 }    	
      	 // la magnifica conservació
      	 else if ( (i<tokens.length-1) &&   	 
      	      ( (matchPostagRegexp(relevantWords2.get(token),ADJECTIU_MS) && matchPostagRegexp(tokens[i+1],NOM_MS) && mArticleELMS.matches() ) 
      	     || (matchPostagRegexp(relevantWords2.get(token),ADJECTIU_FS) && matchPostagRegexp(tokens[i+1],NOM_FS) && mArticleELFS.matches() )
      	     || (matchPostagRegexp(relevantWords2.get(token),ADJECTIU_MP) && matchPostagRegexp(tokens[i+1],NOM_MP) && mArticleELMP.matches() )
      	     || (matchPostagRegexp(relevantWords2.get(token),ADJECTIU_FP) && matchPostagRegexp(tokens[i+1],NOM_FP) && mArticleELFP.matches() ) )
      	    )
      	 {
      	 	  replacement = relevantWords2.get(token).getToken();
      	 }    	
      	 
      }	
      if (replacement != null) {
        msg = "Si \u00E9s un nom o un adjectiu, ha de portar accent: <suggestion>" +replacement+ "</suggestion>.";	
      	
        final RuleMatch ruleMatch = new RuleMatch(this, tokens[i].getStartPos(), tokens[i].getStartPos()+token.length(), msg, "Falta un accent");
        ruleMatches.add(ruleMatch);
      }
    }
    return toRuleMatchArray(ruleMatches);
  }

   /**
   * Match POS tag with regular expression 
   */

   private boolean matchPostagRegexp(AnalyzedTokenReadings aToken, Pattern myPattern)
   { 
   	 boolean matches = false; 
   	 final int readingsLen = aToken.getReadingsLength();
     for (int k = 0; k < readingsLen; k++) {
        final String posTag = aToken.getAnalyzedToken(k).getPOSTag();
        if (posTag!=null)
	      {
	        final Matcher m = myPattern.matcher(posTag);
	        if (m.matches()) {
	            matches = true;
	            break;
	        }
        }     
      }
     return matches;	
   }

  /**
   * Load words.
   */
   private Map<String, AnalyzedTokenReadings> loadWords(InputStream file) throws IOException {
    final Map<String, AnalyzedTokenReadings> map = new HashMap<String, AnalyzedTokenReadings>();
    final Scanner scanner = new Scanner(file, FILE_ENCODING);
    try {
      while (scanner.hasNextLine()) {
        final String line = scanner.nextLine().trim();
        if (line.length() < 1) {
          continue;
        }
        if (line.charAt(0) == '#') {      // ignore comments
          continue;
        }
        final String[] parts = line.split(";");
        if (parts.length != 3) {
          throw new IOException("Format error in file " + JLanguageTool.getDataBroker().getFromRulesDirAsUrl(FILE_NAME) + ", line: " + line);
        }
        map.put(parts[0], new AnalyzedTokenReadings( new AnalyzedToken(parts[1],parts[2],"lemma"),0));
       // map.put(parts[1], parts[0]);
      }
    } finally {
      scanner.close();
    }
    return map;
  }

  @Override
  public void reset() {
    // nothing
  }
}

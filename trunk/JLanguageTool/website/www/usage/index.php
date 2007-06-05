<?php
$page = "usage";
$title = "LanguageTool";
$title2 = "Usage";
$lastmod = "2007-06-02 15:00:00 CET";
include("../../include/header.php");
?>
		
<p><strong>Installation and Usage outside OpenOffice.org</strong></p>

<p>See <?=show_link("Overview", "./", 0)?> for a description of how to use LanguageTool
with OpenOffice.org.</p>

<ul class="largelist">

	<li><strong>As a stand-alone application</strong>:
	Unzip the archive. Amongst others,
	an archive called <tt>standalone-libs.zip</tt> will be created. Unzip that, too.
	Then start <tt>LanguageToolGUI.jar</tt> by double clicking on it. If your computer isn't
	configured to start jar archives, start it from the command line using<br />
	<tt>java -jar LanguageToolGUI.jar</tt><br />
	You can use the <tt>--tray</tt> option to start LanguageTool inside the system tray.
	</li>

	<li><strong>As a stand-alone application on the command line</strong>:
	see above, but start LanguageTool.jar using<br />
	<tt>java -jar LanguageTool.jar &lt;filename></tt><br />
	LanguageTool only supports plain text files.</li>

	<li><strong>Embedding LanguageTool in Java applications:</strong> See
	<?=show_link("the API documentation", "api/", 0) ?>. You just need to create a JLanguageTool object and use that
	to check your text.</li>

	<li><strong>Using LanguageTool from other applications:</strong> Start the stand-alone
	application and configure it to listen on a port that is not used yet (the default
	port, 8081, should often be okay). This way LanguageTool will run in server mode
	until you stop it. <br />
	The client that wants to use LanguageTool can now just send its text to this URL:<br />
	<tt>http://localhost:8081/?language=xx&amp;text=my+text</tt><br />
	The <tt>language</tt> parameter must specify the two-character language code
	(the language of the text to be checked). The <tt>text</tt> parameter is the
	text itself (you may need to encode it for URLs). You can use both POST and
	GET to send your requests to the LanguageTool server.<br />
	For the input "this is a test" the LanguageTool server will reply with this
	XML response:
	
<pre>
&lt;?xml version="1.0" encoding="UTF-8"?>
&lt;matches>
&lt;error fromy="0" fromx="0" toy="0" tox="5" 
  ruleId="UPPERCASE_SENTENCE_START" 
  msg="This sentence does not start with an uppercase letter" 
  replacements="This" context="this is a test." 
  contextoffset="0"
  errorlength="4"/>
&lt;/matches>
</pre>

	<p>The server can also be started on the command line using this command:<br />
	<tt>java -cp jaminid.jar:LanguageTool.jar de.danielnaber.languagetool.server.HTTPServer</tt>

	</li>

</ul>

<?php
include("../../include/footer.php");
?>

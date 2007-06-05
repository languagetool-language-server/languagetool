<?php
function show_link($title, $url, $show_alt, $title_attr="") {
	global $homepage;
	$html = "";
	$alt = "";
	$img_path = "/images";
	if( $homepage ) {
		$img_path = "art";
	}
	if( strpos($url, "mailto:") === false && $show_alt ) {
		if( strpos($url, "http:") === false ) {
			$alt = "internal link to ".$title;
		} else {
			$alt = "external link to ".$title;
		}
	} else if ( $show_alt ) {
		$alt = "email";
	}
	if( $title_attr ) {
		$title_attr = 'title="'.$title_attr.'"';
	}
	if( strpos($url, "http:") === false ) {
		$html .= '<a '.$title_attr.' href="'.$url.'"><img src="'.$img_path.'/link.png" border="0" hspace="2" width="8" height="9" alt="'.$alt.'" />';
	} else {
		$html .= '<a '.$title_attr.' href="'.$url.'"><img src="'.$img_path.'/link_extern.png" border="0" hspace="2" width="7" height="9" alt="'.$alt.'" />';
	}
	$html .= $title.'</a>';
	return $html;
}

function getmicrotime() { 
	list($usec, $sec) = explode(" ",microtime()); 
	return ((float)$usec + (float)$sec); 
}

function send_last_modified_header() {
	global $last_update_full;
	$timestamp = strtotime($last_update_full);
	header("Last-Modified: ".date("D, j M Y G:i:s T", $timestamp));
}

?>

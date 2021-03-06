/*******************************************************************************
 Copyright (c) 2017 KTH Royal Institute of Technology.

  All rights reserved. This program and the accompanying materials
are made available under the terms of the Eclipse Public License v1.0
and Eclipse Distribution License v. 1.0 which accompanies this distribution.

  The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
  and the Eclipse Distribution License is available at
http://www.eclipse.org/org/documents/edl-v10.php.

  Contributors:

    Fr�d�ric Loiret     - Switch the template to Bootstrap (519699)
    Andrii Berezovskyi  - Support for UI Preview (494303)

This file is generated by org.eclipse.lyo.oslc4j.codegenerator
 *******************************************************************************/

/* parse OSLC UI Preview XML into JSON structure with uri, title, h and w */
function parsePreview(xml) {
  var ret = {};
  var compact = firstChild(firstChild(xml));

  var titleChild = firstChildNamed(compact, 'dcterms:title');
  ret.title = titleChild.textContent;

  var smallPrev = firstChildNamed(compact, 'oslc:smallPreview');
  var largePrev = firstChildNamed(compact, 'oslc:largePreview');
  var preview;
  if (smallPrev !== null) {
    preview = firstChild(smallPrev);
  } else {
    preview = firstChild(largePrev);
  }
  if (preview) {
    var document = firstChildNamed(preview, 'oslc:document');
    if (document) ret.uri = document.getAttribute('rdf:resource');
    var height = firstChildNamed(preview, 'oslc:hintHeight');
    ret.height = height.textContent;
    var width = firstChildNamed(preview, 'oslc:hintWidth');
    ret.width = width.textContent;
  }
  return ret;
}

function firstChild(e) {
  for (i = 0; i < e.childNodes.length; i++) {
    if (e.childNodes[i].nodeType === Node.ELEMENT_NODE) {
      return e.childNodes[i];
    }
  }
}

function firstChildNamed(e, nodeName) {
  for (i = 0; i < e.childNodes.length; i++) {
    if (e.childNodes[i].nodeType === Node.ELEMENT_NODE
      && e.childNodes[i].nodeName === nodeName) {
      return e.childNodes[i];
    }
  }
}

$(function () {

  var previewLinks = $("a.oslc-resource-link");
  previewLinks.popover({
    container: "body",
    content: "Loading...",
    delay: {"show": 120, "hide": 60},
    html: true,
    placement: "auto",
    trigger: "hover"
  });


  previewLinks.on("show.bs.popover", function () {
    var uiElem = $(this);
    var popoverElem = uiElem.data('bs.popover');

    $.ajax({
      type: "GET",
      url: this.getAttribute("href"),
      dataType: "xml",
      accepts: {
        xml: "application/x-oslc-compact+xml"
      },
      success: function (data) {
        try {
          var previewData = parsePreview(data);
          var html = "<iframe src='" + previewData.uri + "' ";
          var w = previewData.width ? previewData.width : "45em";
          var h = previewData.height ? previewData.height : "11em";
          html += " style='border:0px; height:" + h + "; width:" + w + "'";
          html += "></iframe>";

          uiElem.attr('data-original-title', previewData.title);
          uiElem.attr('data-content', html);
          popoverElem.setContent();
        } catch (e) {
          uiElem.attr('data-original-title', "Error");
          uiElem.attr('data-content', '<b>Error parsing preview dialog info</b>');
          popoverElem.setContent();
        }
      },
      error: function (xhr, status, err) {
        uiElem.attr('data-original-title', "Error");
        uiElem.attr('data-content', '<b>Error loading the preview dialog</b>');
        popoverElem.setContent();
      }
    });
  })
});

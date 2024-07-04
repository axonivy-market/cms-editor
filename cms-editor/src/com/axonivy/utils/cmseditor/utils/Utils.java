package com.axonivy.utils.cmseditor.utils;

import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Entities.EscapeMode;

public class Utils {

  private static final String HTML_TAG_PATTERN = "<.*?>";
  private static final String REMOVE_HTML_TAG_PATTERN = "<[^>]+>";
  private static final String TABLE_ELEMENT = "table";
  private static final String UNORDERED_PATTERN = "<ul> %s </ul>";
  private static final String LIST_ITEM_PATTERN = "<li style='padding:0 2rem 0.25rem 0;'> %s </li>";

  public static String reformatHTML(String originalContent, String content) {
    if (containsHtmlTag(originalContent)) {
      var originalDoc = Jsoup.parseBodyFragment(content);
      var doc = Jsoup.parseBodyFragment(content);
      migrateTableAttr(originalDoc, doc);
      doc.outputSettings().escapeMode(EscapeMode.xhtml).prettyPrint(true);
      return doc.body().html();
    } else {
      return removeTags(content);
    }
  }

  public static boolean containsHtmlTag(String str) {
    if (Objects.isNull(str)) {
      return false;
    }
    var pattern = Pattern.compile(HTML_TAG_PATTERN);
    return pattern.matcher(str).find();
  }

  public static String removeTags(String text) {
    if (Objects.isNull(text)) {
      return StringUtils.EMPTY;
    }
    var pattern = Pattern.compile(REMOVE_HTML_TAG_PATTERN);
    return pattern.matcher(text).replaceAll(StringUtils.EMPTY);
  }

  private static void migrateTableAttr(Document originalDoc, Document doc) {
    var originalTables = doc.select(TABLE_ELEMENT);
    var tables = doc.select(TABLE_ELEMENT);
    var minSize = Math.min(originalTables.size(), tables.size());

    for (var i = 0; i < minSize; i++) {
      var originalTable = originalTables.get(i);
      var targetTable = tables.get(i);

      // Copy attributes from originalTable to targetTable
      for (var attr : originalTable.attributes()) {
        targetTable.attr(attr.getKey(), attr.getValue());
      }
    }
  }

  public static String convertListToHTMLList(List<String> stringList) {
    var htmlStringBuilder = new StringBuilder();
    for (String item : stringList) {
      htmlStringBuilder.append(String.format(LIST_ITEM_PATTERN, item));
    }

    return String.format(UNORDERED_PATTERN, htmlStringBuilder.toString());
  }
}

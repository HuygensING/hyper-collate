package nl.knaw.huygens.hypercollate.model;

import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

public class Markup {
  String tagname;
  Map<String, String> attributeMap = new TreeMap<>();

  public Markup(String tagName) {
    this.tagname = tagName;
  }

  public Markup addAttribute(String key, String value) {
    attributeMap.put(key, value);
    return this;
  }

  public Optional<String> getAttributeValue(String key) {
    return Optional.ofNullable(attributeMap.get(key));
  }

}

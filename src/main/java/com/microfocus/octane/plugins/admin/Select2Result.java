package com.microfocus.octane.plugins.admin;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

//https://select2.org/data-sources/formats
/*{
  "results": [
    {
      "id": 1,
      "text": "Option 1"
    },
    {
      "id": 2,
      "text": "Option 2"
    }
  ],
  "pagination": {
    "more": true
  }
}*/
@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class Select2Result {

    @XmlElement(name = "results")
    private List<Select2ResultItem> results = new ArrayList<>();

    @XmlElement(name = "pagination")
    private HashMap pagination = new HashMap() {{
        put("more", false);
    }};

    public void addItem(String id, String text) {
        results.add(new Select2ResultItem(id, text));
    }
}

package com.microfocus.octane.plugins.admin;

import java.util.ArrayList;
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
public class Select2Result {

    private List<Select2ResultItem> results = new ArrayList<>();


    public void addItem(String id, String text){
        results.add(new Select2ResultItem(id,text));
    }
}

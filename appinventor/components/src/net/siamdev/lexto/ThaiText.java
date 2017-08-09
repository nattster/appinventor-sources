// -*- mode: java; c-basic-offset: 2; -*-
// Copyright 2009-2011 Google, All Rights reserved
// Copyright 2011-2012 MIT, All rights reserved
// Released under the Apache License, Version 2.0
// http://www.apache.org/licenses/LICENSE-2.0

package net.siamdev.lexto;

import com.google.appinventor.components.annotations.DesignerComponent;
import com.google.appinventor.components.annotations.PropertyCategory;
import com.google.appinventor.components.annotations.SimpleEvent;
import com.google.appinventor.components.annotations.SimpleFunction;
import com.google.appinventor.components.annotations.SimpleObject;
import com.google.appinventor.components.annotations.SimpleProperty;
import com.google.appinventor.components.common.ComponentCategory;
import com.google.appinventor.components.common.PropertyTypeConstants;
import com.google.appinventor.components.runtime.Component;
import com.google.appinventor.components.runtime.ComponentContainer;
import com.google.appinventor.components.runtime.AndroidNonvisibleComponent;
import com.google.appinventor.components.runtime.util.AsynchUtil;
import com.google.appinventor.components.runtime.EventDispatcher;

import android.util.Log;
import android.os.Handler;

import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.StringWriter;
import java.io.BufferedReader;

/**
 * Camera provides access to the phone's camera
 *
 *
 */
@DesignerComponent(version = 1,
   description = "Thai Text Utility",
   category = ComponentCategory.EXTENSION,
   nonVisible = true,
   iconName = "images/camera.png")
@SimpleObject(external=true)
public class ThaiText extends AndroidNonvisibleComponent
    implements Component {
  
  private static final String LOG_TAG = "ThaiText";
  private final int BUFFER_LENGTH = 4096;
  private final ComponentContainer container;
  private static LongLexTo ltx;
  private String result = "not read";
  private boolean loadFail = false;
  private final Handler androidUIHandler;

  public ThaiText(ComponentContainer container) {
    super(container.$form());
    this.container = container;
    androidUIHandler = new Handler();
  }


  /**
   * Asynchronously reads from the given file. Calls the main event thread
   * when the function has completed reading from the file.
   * @param filepath the file to read
   * @throws FileNotFoundException
   * @throws IOException when the system cannot read the file
   */
  private void AsyncRead(InputStream fileInput, final String fileName) {
    InputStreamReader input = null;
    try {
      input = new InputStreamReader(fileInput);
      BufferedReader reader = new BufferedReader(input);
      String line;
      while(reader.ready()) {
        while((line=reader.readLine())!=null) {
          line=line.trim();
          if(line.length()>0)
            ltx.addWord(line);
        }
      }
      
      Log.i(LOG_TAG, "Read finished");
      loadFail = false;
    } catch (FileNotFoundException e) {
      Log.e(LOG_TAG, "FileNotFoundException", e);
      loadFail = true;
    } catch (IOException e) {
      Log.e(LOG_TAG, "IOException", e);
      loadFail = true;
    } finally {
      if (input != null) {
        try {
          input.close();
        } catch (IOException e) {
          // do nothing...
        }
      }
    }
  }

  @SimpleFunction
  public void LoadDictionary() {
    if(ltx != null) {
      Log.i(LOG_TAG, "Dictionary already loaded. Skipping.");
      return;
    }
    ltx = new LongLexTo();
    try {
      InputStream inputStream;
      final String fileName = "lexitron.txt";
      inputStream = form.getAssets().open(fileName);
      
      final InputStream asyncInputStream = inputStream;
      AsynchUtil.runAsynchronously(androidUIHandler, new Runnable() {
          @Override
          public void run() {
            AsyncRead(asyncInputStream, fileName);
          }
        }, new Runnable() {
          @Override
          public void run() {
            if(loadFail) {
              LoadFailed();
            } else {
              LoadSuccess();
            }
          }
        });
    } catch (FileNotFoundException e) {
      Log.e(LOG_TAG, "FileNotFoundException", e);
    } catch (IOException e) {
      Log.e(LOG_TAG, "IOException", e);
    }
  }

  @SimpleFunction
  public List<String> SplitWord(String sentence) {
    List<String> result = new ArrayList<String>();
    if(ltx == null) {
      Log.w(LOG_TAG, "Dictionary not load, unable to split word");
      return result;
    }
    ltx.wordInstance(sentence);
    int begin, end;
    begin=ltx.first();
    int i=0;
    while(ltx.hasNext()) {
      end=ltx.next();
      result.add(sentence.substring(begin, end));
      begin=end;
    }
    return result;
  }

  @SimpleEvent
  public void LoadSuccess() {
    EventDispatcher.dispatchEvent(this, "LoadSuccess");
  }

  @SimpleEvent
  public void LoadFailed() {
    EventDispatcher.dispatchEvent(this, "LoadFailed");
  }
}

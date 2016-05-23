/*
 * Copyright 2016 The Depan Project Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.devtools.depan.graphml.eclipse;


import com.google.devtools.depan.graph_doc.eclipse.ui.wizards.AbstractAnalysisPage;

import com.google.common.base.Strings;

import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import java.io.File;

/**
 * The "New" wizard page allows setting the container for the new file as well
 * as the file name. The page will only accept file name without the extension
 * OR with the extension that matches the expected one (dgi).
 * 
 * @author <a href="mailto:leeca@pnambic.com">Lee Carver</a>
 */
public class NewGraphMLPage extends AbstractAnalysisPage {

  public static final String PAGE_LABEL = "New GraphML Analysis";

  private static final String[] POM_FILTER = new String[] { "*.graphml" };

  // UI elements for embedded analysis source part
  private Text pathEntry;
  private Button pathBrowse;
  private Combo processCombo;

  private String errorMsg;

  /**
   * @param selection
   */
  public NewGraphMLPage(ISelection selection) {
    super(selection, PAGE_LABEL,
        "This wizard creates a new dependency graph"
        + " from an analysis of a GraphML file.",
        createFilename("GraphML"));
  }

  @Override
  public String getAnalysisSourceErrorMsg() {
    return errorMsg;
  }

  /**
   * @param container
   */
  @Override
  protected Composite createSourceControl(Composite container) {

    // group for selecting jar file or Directory as input
    Group source = new Group(container, SWT.NONE);
    source.setText("Graph ML");

    GridLayout grid = new GridLayout();
    grid.numColumns = 3;
    grid.verticalSpacing = 9;
    source.setLayout(grid);

    // First row: directory path selector
    @SuppressWarnings("unused")
    Label pathLabel = createSimpleLabel(source, "&GraphML:");
    pathEntry = new Text(source, SWT.BORDER | SWT.SINGLE);
    pathEntry.setLayoutData(createHorzFillData());
    pathEntry.addModifyListener(new ModifyListener() {

      @Override
      public void modifyText(ModifyEvent e) {
        dialogChanged();
      }
    });
    pathBrowse = new Button(source, SWT.PUSH);
    pathBrowse.setText("Browse...");
    pathBrowse.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        handleBrowse();
      }
    });

    // Second row: compute or use
    @SuppressWarnings("unused")
    Label procLabel = createSimpleLabel(source, "Processing:");
    processCombo = createProcessCombo(source);
    GridData processData = createColSpanData(2);
    processData.grabExcessHorizontalSpace = false;
    processCombo.setLayoutData(processData);

    errorMsg = validateInputs();

    return source;
  }

  private Combo createProcessCombo(Composite container) {
    Combo result = new Combo(container, SWT.DROP_DOWN | SWT.READ_ONLY);

    for (GraphMLProcessing item : GraphMLProcessing.values()) {
      result.add(item.label);
    }
    result.select(0);
    return result;
  }

  /**
   * Ensures that all inputs are valid.
   */
  private void dialogChanged() {
    errorMsg = validateInputs();
    updateStatus(errorMsg);
  }

  /**
   * Determine if the page has been filled in correctly.
   */
  private String validateInputs() {
    String pomPath = pathEntry.getText();
    if (Strings.isNullOrEmpty(pomPath)) {
      return "GraphML file cannot be empty";
    }
    File pathFile = new File(pomPath);
    if (!pathFile.exists()) {
      return "GraphML file doesn't exist";
    }

    // No problems.
    return null;
  }

  /**
   * Open a directory and write the name in the given {@link Text} object.
   */
  private void handleBrowse() {
    FileDialog dialog = new FileDialog(getShell(), SWT.OPEN);
    dialog.setFilterExtensions(POM_FILTER);
    pathEntry.setText(dialog.open());
  }

  /////////////////////////////////////
  // Provide access to user data in the fields.

  public String getPathText() {
    return pathEntry.getText();
  }

  public File getPathFile() {
    return new File(getPathText());
  }

  public String getTreePrefix() {
    File path = getPathFile();

    return path.getPath();
  }

  public GraphMLProcessing getProcessing() {
    int select = processCombo.getSelectionIndex();
    return GraphMLProcessing.values()[select];
  }
}

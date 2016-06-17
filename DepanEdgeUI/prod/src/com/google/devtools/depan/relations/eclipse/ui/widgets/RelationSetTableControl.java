/*
 * Copyright 2014 The Depan Project Authors
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

package com.google.devtools.depan.relations.eclipse.ui.widgets;

import com.google.devtools.depan.graph.api.Relation;
import com.google.devtools.depan.platform.AlphabeticSorter;
import com.google.devtools.depan.platform.InverseSorter;
import com.google.devtools.depan.platform.LabelProviderToString;
import com.google.devtools.depan.platform.PlatformResources;
import com.google.devtools.depan.platform.eclipse.ui.tables.EditColTableDef;
import com.google.devtools.depan.relations.models.RelationSetRepository;

import com.google.common.collect.Sets;

import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.CheckboxCellEditor;
import org.eclipse.jface.viewers.ICellModifier;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerSorter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;

/**
 * Run a view of the known relations as its own reusable "part".
 */
public class RelationSetTableControl extends Composite {

  public static final String COL_NAME = "Name";
  public static final String COL_SOURCE = "Source";
  public static final String COL_VISIBLE = "Visible";

  /** To indicate that the "Visible" column should be updated. */
  public static final String[] UPDATE_VISIBLE = {COL_VISIBLE};

  public static final int INDEX_NAME = 0;
  public static final int INDEX_SOURCE = 1;
  public static final int INDEX_VISIBLE = 2;

  private static final EditColTableDef[] TABLE_DEF = new EditColTableDef[] {
    new EditColTableDef(COL_NAME, false, COL_NAME, 180),
    new EditColTableDef(COL_SOURCE, false, COL_SOURCE, 80),
    new EditColTableDef(COL_VISIBLE, true, COL_VISIBLE, 70),
  };

  private class ControlRelationVisibleListener
      implements RelationSetRepository.ChangeListener {

    @Override
    public void includedRelationChanged(Relation relation, boolean visible) {
      viewer.update(relation, UPDATE_VISIBLE);
    }
  }

  private ControlRelationVisibleListener relSetListener;

  private RelationSetRepository relSetRepo;

  private TableViewer viewer;

  public RelationSetTableControl(Composite parent) {
    super(parent, SWT.NONE);

    GridLayout gridLayout = new GridLayout();
    gridLayout.marginHeight = 0;
    gridLayout.marginWidth = 0;
    setLayout(gridLayout);

    viewer = new TableViewer(this,
        SWT.FULL_SELECTION | SWT.MULTI | SWT.BORDER | SWT.V_SCROLL);
    // set up label provider
    viewer.setLabelProvider(new RelEditorLabelProvider());

    // Set up layout properties
    Table relTableControl = viewer.getTable();
    relTableControl.setLayoutData(
        new GridData(SWT.FILL, SWT.FILL, true, true));
    relTableControl.setToolTipText("List of Relations");

    // initialize the table
    relTableControl.setHeaderVisible(true);
    EditColTableDef.setupTable(TABLE_DEF, relTableControl);

    // Configure cell editing
    CellEditor[] cellEditors = new CellEditor[6];
    cellEditors[INDEX_NAME] = null;
    cellEditors[INDEX_SOURCE] = null;
    cellEditors[INDEX_VISIBLE] = new CheckboxCellEditor(relTableControl);

    viewer.setCellEditors(cellEditors);
    viewer.setColumnProperties(EditColTableDef.getProperties(TABLE_DEF));
    viewer.setCellModifier(new RelEditorCellModifierHandler());

    // TODO: Add column sorters, filters?
    configSorters(relTableControl);

    // Configure content last: use updateTable() to render relations
    viewer.setContentProvider(ArrayContentProvider.getInstance());
  }

  /**
   * Fill the list with {@link Relation}s.
   */
  public void setInput(Collection<Relation> relations) {

    // Since rendering depends on relPlugin, set input after
    // relPlugin is updated.
    viewer.setInput(relations);
  }

  @SuppressWarnings("unchecked")
  public Collection<Relation> getInput() {
    return (Collection<Relation>) viewer.getInput();
  }

  public void setVisibiltyRepository(RelationSetRepository visRepo) {
    this.relSetRepo = visRepo;
    relSetListener = new ControlRelationVisibleListener();
    visRepo.addChangeListener(relSetListener);
  }

  public void removeRelSetRepository(RelationSetRepository visRepo) {
    if (null != relSetListener) {
      this.relSetRepo.removeChangeListener(relSetListener);
      relSetListener = null;
    }
    relSetListener = null;
    this.relSetRepo = null;
  }

  /////////////////////////////////////
  // Convenience for modifying visibility property.
  // Useful for buttons external to the table (e.g. clear all)

  public void clearVisibleSelection() {
    clearRelations(getSelectedRelations());
  }

  public void invertVisibleSelection() {
    invertRelations(getSelectedRelations());
  }

  public void checkVisibleSelection() {
    checkRelations(getSelectedRelations());
  }

  public void clearVisibleTable() {
    clearRelations(getInput());
  }

  public void invertVisibleTable() {
    invertRelations(getInput());
  }

  public void checkVisibleTable() {
    checkRelations(getInput());
  }

  public void clearRelations(Collection<Relation> relations) {
    for (Relation relation : relations) {
      relSetRepo.setRelationChecked(relation, false);
    }
  }

  public void invertRelations(Collection<Relation> relations) {
    for (Relation relation : relations) {
      boolean wasChecked = relSetRepo.isRelationIncluded(relation);
      relSetRepo.setRelationChecked(relation, !wasChecked);
    }
  }

  public void checkRelations(Collection<Relation> relations) {
    for (Relation relation : relations) {
      relSetRepo.setRelationChecked(relation, true);
    }
  }

  /**
   * Invert the set of relations selected in the table.
   * Don't change the state of any relation.
   */
  public void invertSelectedRelations() {
    ISelection selection = viewer.getSelection();
    if (!(selection instanceof IStructuredSelection)) {
      return;
    }

    IStructuredSelection structuredSelection = (IStructuredSelection) selection;
    Collection<Relation> inverse =
        computeInverseRelations(getInput(), structuredSelection);

    StructuredSelection nextSelection =
        new StructuredSelection(inverse.toArray());
    viewer.setSelection(nextSelection, true);
  }

  private Collection<Relation> getSelectedRelations() {
    ISelection selection = viewer.getSelection();
    if (!(selection instanceof IStructuredSelection)) {
      return Collections.emptyList();
    }

    Collection<Relation> result = Sets.newHashSet();
    @SuppressWarnings("rawtypes")
    Iterator iter = ((IStructuredSelection) selection).iterator();
    while (iter.hasNext()) {
      result.add((Relation) iter.next());
    }
    return result;
  }

  private Collection<Relation> computeInverseRelations(
      Collection<Relation> universe, IStructuredSelection selection) {

    if (selection.isEmpty()) {
      return universe;
    }

    Collection<Relation> inverse = Sets.newHashSet(universe);
    @SuppressWarnings("rawtypes")
    Iterator iter = selection.iterator();
    while (iter.hasNext()) {
      inverse.remove((Relation) iter.next());
    }
    return inverse;
  }

  /////////////////////////////////////
  // Column sorting

  private void configSorters(Table table) {
    int index = 0;
    for (TableColumn column : table.getColumns()) {
      final int colIndex = index++;

      column.addSelectionListener(new SelectionAdapter() {
        @Override
        public void widgetSelected(SelectionEvent event) {
          updateSortColumn((TableColumn) event.widget, colIndex);
        }
      });
    }
  }

  private void updateSortColumn(TableColumn column, int colIndex) {
    setSortColumn(column, colIndex, getSortDirection(column));
  }

  private int getSortDirection(TableColumn column) {
    Table tableControl = (Table) viewer.getControl();
    if (column != tableControl.getSortColumn()) {
      return SWT.DOWN;
    }
    // If it is unsorted (SWT.NONE), assume down sort
    return (SWT.DOWN == tableControl.getSortDirection())
        ? SWT.UP : SWT.DOWN;
  }

  private void setSortColumn(
      TableColumn column, int colIndex, int direction) {

    ViewerSorter sorter = buildColumnSorter(colIndex);
    if (SWT.UP == direction) {
      sorter = new InverseSorter(sorter);
    }

    Table tableControl = (Table) viewer.getControl();
    viewer.setSorter(sorter);
    tableControl.setSortColumn(column);
    tableControl.setSortDirection(direction);
  }

  private ViewerSorter buildColumnSorter(int colIndex) {
    if (INDEX_VISIBLE == colIndex) {
      return new BooleanViewSorter();
    }

    // By default, use an alphabetic sort over the column labels.
    ITableLabelProvider labelProvider =
        (ITableLabelProvider) viewer.getLabelProvider();
    ViewerSorter result = new AlphabeticSorter(
        new LabelProviderToString(labelProvider, colIndex));
    return result;
  }

  private class BooleanViewSorter extends ViewerSorter {

    @Override
    public int compare(Viewer viewer, Object e1, Object e2) {
      boolean vis1 = isVisible(e1);
      boolean vis2 = isVisible(e2);
      return Boolean.compare(vis1, vis2);
    }

    private boolean isVisible(Object e1) {
      if (!(e1 instanceof Relation)) {
        return false;
      }
      return relSetRepo.isRelationIncluded((Relation) e1);
    }
  }

  /////////////////////////////////////
  // Label provider for table cell text

  private class RelEditorLabelProvider extends LabelProvider
      implements ITableLabelProvider {

    @Override
    public String getColumnText(Object element, int columnIndex) {
      if (element instanceof Relation) {
        Relation relation = (Relation) element;
        switch (columnIndex) {
        case INDEX_NAME:
          return relation.toString();
        case INDEX_SOURCE:
          return getSourceLabelForRelation(relation);
        }
      }
      return null;
    }

    private String getSourceLabelForRelation(Relation relation) {
      ClassLoader loader = relation.getClass().getClassLoader();
      String result = loader.toString();
      int bound = result.length();
      int start = Math.max(0, bound - 12);
      return result.substring(start);

      // TODO: More like this, with an relation registry
      // SourcePlugin plugin = relPlugin.get(relation);
      // SourcePluginRegistry registry = SourcePluginRegistry.getInstance();
      // String sourceId = registry.getPluginId(plugin);
      // return registry.getSourcePluginEntry(sourceId).getSource();
    }

    @Override
    public Image getColumnImage(Object element, int columnIndex) {
      if (!(element instanceof Relation)) {
        return null;
      }

      switch (columnIndex) {
      case INDEX_VISIBLE:
        boolean isVis = relSetRepo.isRelationIncluded((Relation) element);
        return PlatformResources.getOnOff(isVis);
      }
      return null;
    }
  }

  /////////////////////////////////////
  // Value provider/modifier for edit cells

  private class RelEditorCellModifierHandler implements ICellModifier{

    @Override
    public boolean canModify(Object element, String property) {
      return EditColTableDef.get(TABLE_DEF, property).isEditable();
    }

    @Override
    public Object getValue(Object element, String property) {
      if (!(element instanceof Relation)) {
        return null;
      }
      Relation relation = (Relation) element;
      if (COL_VISIBLE.equals(property)) {
        return relSetRepo.isRelationIncluded(relation);
      }
      return null;
    }

    @Override
    public void modify(Object element, String property, Object value) {
      if (!(element instanceof TableItem)) {
        return;
      }
      Object modifiedObject = ((TableItem) element).getData();
      if (!(modifiedObject instanceof Relation)) {
        return;
      }
      if (property.equals(COL_VISIBLE) && (value instanceof Boolean)) {
        Relation relation = (Relation) modifiedObject;
        relSetRepo.setRelationChecked(
            relation, ((Boolean) value).booleanValue());
        viewer.update(relation, UPDATE_VISIBLE);
      }
    }
  }

  public void setSelection(ISelection selection) {
    viewer.setSelection(selection);
  }

  public void refresh(boolean refresh) {
    viewer.refresh(refresh);
  }
}

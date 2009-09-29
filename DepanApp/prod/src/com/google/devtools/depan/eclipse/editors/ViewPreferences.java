/*
 * Copyright 2009 Google Inc.
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

package com.google.devtools.depan.eclipse.editors;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.devtools.depan.eclipse.trees.NodeTreeProvider;
import com.google.devtools.depan.eclipse.utils.ListenerManager;
import com.google.devtools.depan.eclipse.visualization.layout.Layouts;
import com.google.devtools.depan.graph.api.DirectedRelationFinder;
import com.google.devtools.depan.model.GraphEdge;
import com.google.devtools.depan.model.GraphModel;
import com.google.devtools.depan.model.GraphNode;
import com.google.devtools.depan.view.CollapseData;
import com.google.devtools.depan.view.Collapser;
import com.google.devtools.depan.view.EdgeDisplayProperty;
import com.google.devtools.depan.view.NodeDisplayProperty;
import com.google.devtools.depan.view.TreeModel;

import java.awt.geom.Point2D;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Logger;

/**
 * Persistent user preferences for presentation of a graph view.  This is the
 * sole means to save and load the graph's presentation, and all preference
 * setting must be member fields of this class.
 * 
 * @author <a href="mailto:leeca@google.com">Lee Carver</a>
 */
public class ViewPreferences {

  private static final Logger logger =
      Logger.getLogger(ViewPreferences.class.getName());

  /////////////////////////////////////
  // Persisted instance members

  /**
   * Rendering location for nodes.
   */
  private Map<GraphNode, Point2D> nodeLocations;

  /**
   * Rendering properties for nodes.
   */
  public Map<GraphNode, NodeDisplayProperty> nodeProperties;

  /**
   * Hash map that contains a list of edge display property objects for each
   * edge in graph.
   */
  private Map<GraphEdge, EdgeDisplayProperty> edgeProperties;

  /**
   * Manager object for handling all collapsed nodes.
   */
  private Collapser collapser;

  private Collection<GraphNode> selectedNodes = ImmutableList.of();

  private Layouts selectedLayout;

  private String description;

  /**
   * Relation finder used by layout algorithms by default if it wasn't
   * specified.
   */
  private DirectedRelationFinder layoutFinder;

  /**
   * Defines the relationship set used to define the view hierarchy
   */
  private DirectedRelationFinder treeRelationshipSet;

  /////////////////////////////////////
  // Transient instance members

  /**
   * Manage objects that are interested in preference changes.
   * Initialization moved to an explicit public method since XStream
   * unmarshalling does not set this.
   */
  private transient ListenerManager<ViewPrefsListener> listeners;

  /////////////////////////////////////
  // Listeners for structures changes

  private abstract static class SimpleDispatcher
      implements ListenerManager.Dispatcher<ViewPrefsListener> {
    public void captureException(RuntimeException errAny) {
      logger.warning(errAny.toString());
    }
  }

  public void addListener(ViewPrefsListener listener) {
    listeners.addListener(listener);
  }

  public void removeListener(ViewPrefsListener listener) {
    listeners.removeListener(listener);
  }

  /////////////////////////////////////
  // Instance constructors and factories

  /**
   * Explicit simple constructor
   */
  public ViewPreferences() {
    this(
        Maps.<GraphNode, Point2D>newHashMap(),
        Maps.<GraphNode, NodeDisplayProperty>newHashMap(),
        Maps.<GraphEdge, EdgeDisplayProperty>newHashMap(),
        ImmutableList.<GraphNode>of(),
        "");
  }

  public ViewPreferences(
      Map<GraphNode, Point2D> newNodeLocations,
      Map<GraphNode, NodeDisplayProperty> newNodeProperties,
      Map<GraphEdge, EdgeDisplayProperty> newEdgeProperties,
      Collection<GraphNode> newSelectedNodes,
      String newDescription) {
    initTransients();
    collapser = new Collapser();

    this.nodeLocations = newNodeLocations;
    this.nodeProperties = newNodeProperties;
    this.edgeProperties = newEdgeProperties;
    this.selectedNodes = newSelectedNodes;
    this.description = newDescription;
  }

  /**
   * Initialize transient fields.  This is used directly in the XStream
   * unmarshalling converter, since none of the constructors are actually
   * invoked.
   */
  public void initTransients() {
    listeners = new ListenerManager<ViewPrefsListener>();
  }

  /**
   * Construct a new set of ViewPreferences by copying any preferences
   * in the current view.  The only preferences (including location) that
   * are copied are those for the indicated nodes.  For example, only edge
   * properties that include both nodes are included in the result.
   * 
   * @param source source of node properties
   * @param nodes selector for properties to retain
   * @return new view preferences for indicated nodes
   */
  public static ViewPreferences buildFilteredNodes(
      ViewPreferences source, Collection<GraphNode> nodes) {
    Map<GraphNode, Point2D> newNodeLocations = 
        filterMap(nodes, source.nodeLocations);

    Map<GraphNode, NodeDisplayProperty> newNodeProperties =
        filterMap(nodes, source.nodeProperties);

    Map<GraphEdge, EdgeDisplayProperty> newEdgeProperties =
        Maps.newHashMap();
    for (Entry<GraphEdge, EdgeDisplayProperty> entry : 
        source.edgeProperties.entrySet()) {
      GraphEdge edge = entry.getKey();
      if (nodes.contains(edge.getHead()) && nodes.contains(edge.getTail())) {
        newEdgeProperties.put(entry.getKey(), entry.getValue());
      }
    }

    Collection<GraphNode> newSelectedNodes = Lists.newArrayList();
    for (GraphNode node : source.selectedNodes) {
      if (nodes.contains(node)) {
        newSelectedNodes.add(node);
      }
    }

    String newDescription = (source.description.isEmpty())
        ? "" : "Derived from " + source.description;

    return new ViewPreferences(newNodeLocations, newNodeProperties,
        newEdgeProperties, newSelectedNodes, newDescription);
  }

  private static <K, V> Map<K, V> filterMap(
      Collection<K> filter, Map<K, V> source) {
    Map<K, V> result = Maps.newHashMap();
    for (Entry<K, V> entry : source.entrySet()) {
      if (filter.contains(entry.getKey())) {
        result.put(entry.getKey(), entry.getValue());
      }
    }
    return result;
  }

  /////////////////////////////////////
  // Simple accessors (event-less)

  public Map<GraphNode, Point2D> getNodeLocations() {
    return nodeLocations;
  }

  public void setNodeLocations(final Map<GraphNode, Point2D> newLocations) {
    nodeLocations = newLocations;

    listeners.fireEvent(new SimpleDispatcher() {
      @Override
      public void dispatch(ViewPrefsListener listener) {
        listener.nodeLocationsChanged(newLocations);
      }
    });
  }

  public DirectedRelationFinder getLayoutFinder() {
    return layoutFinder;
  }

  /////////////////////////////////////
  // Manipulate the nodes

  public NodeDisplayProperty getNodeProperty(GraphNode node) {
    return nodeProperties.get(node);
  }

  public void setNodeProperty(
      final GraphNode node, final NodeDisplayProperty newProperty) {
    nodeProperties.put(node, newProperty);

    listeners.fireEvent(new SimpleDispatcher() {
      @Override
      public void dispatch(ViewPrefsListener listener) {
        listener.nodePropertyChanged(node, newProperty);
      }
    });
  }

  public NodeTreeProvider<NodeDisplayProperty>
      getNodeDisplayPropertyProvider() {
    return new NodeDisplayPropertyProvider();
  }

  private class NodeDisplayPropertyProvider
      implements NodeTreeProvider<NodeDisplayProperty> {

    @Override
    // TODO(leeca): consolidate with ViewEditor.getNodeProperty()
    public NodeDisplayProperty getObject(GraphNode node) {
      NodeDisplayProperty result = getNodeProperty(node);
      if (null != result) {
        return result;
      }
      return new NodeDisplayProperty();
    }
  }

  /////////////////////////////////////
  // Manipulate the selected nodes

  public Collection<GraphNode> getSelectedNodes() {
    return selectedNodes;
  }

  public void setSelectedNodes(
      Collection<GraphNode> newSelection, final Object author) {
    final Collection<GraphNode> previous = selectedNodes;

    selectedNodes = newSelection;

    listeners.fireEvent(new SimpleDispatcher() {
      @Override
      public void dispatch(ViewPrefsListener listener) {
        listener.selectionChanged(previous, selectedNodes, author);
      }
    });
  }

  public void editSelectedNodes(
      Collection<GraphNode> removeNodes, Collection<GraphNode> addNodes,
      final Object author) {
    final Collection<GraphNode> previous = selectedNodes;

    Collection<GraphNode> working = Lists.newArrayList(selectedNodes);
    working.removeAll(removeNodes);
    working.addAll(addNodes);

    selectedNodes = working;

    listeners.fireEvent(new SimpleDispatcher() {
      @Override
      public void dispatch(ViewPrefsListener listener) {
        listener.selectionChanged(previous, selectedNodes, author);
      }
    });
  }

  /////////////////////////////////////
  // Manipulate the edges

  public EdgeDisplayProperty getEdgeProperty(GraphEdge edge) {
    return edgeProperties.get(edge);
  }

  public void setEdgeProperty(
      final GraphEdge edge, final EdgeDisplayProperty newProperty) {
    edgeProperties.put(edge, newProperty);

    listeners.fireEvent(new SimpleDispatcher() {
      @Override
      public void dispatch(ViewPrefsListener listener) {
        listener.edgePropertyChanged(edge, newProperty);
      }
    });
  }

  /////////////////////////////////////
  // Manipulate the description

  public String getDescription() {
    return description;
  }

  public void setDescription(String newDescription) {
    description = newDescription;

    listeners.fireEvent(new SimpleDispatcher() {
      @Override
      public void dispatch(ViewPrefsListener listener) {
        listener.descriptionChanged(description);
      }
    });
  }

  /////////////////////////////////////
  // Manipulate the collapsed state

  /**
   * Based on the collapsing preferences, compute the set of nodes and
   * edges that are exposed in a graph.
   * 
   * @param graph source of nodes to collapse
   * @return graph containing only uncollapsed nodes
   */
  public GraphModel getExposedGraph(GraphModel graph) {
    return collapser.buildExposedGraph(graph);
  }

  /**
   * Collapse a set of nodes under a specific master node.
   *
   * @param master node to represent collapsed nodes.  Should be in picked.
   * @param picked collection of nodes to collapse
   * @param erase erase (and merge) any collapsed nodes in picked as part of
   * the new collapsed node
   * @param author the initiator for this process
   */
  public void collapse(
      GraphNode master,
      Collection<GraphNode> picked,
      boolean erase, Object author) {
    collapser.collapse(master, picked, erase);

    fireCollapseChanged(
        Collections.singleton(collapser.getCollapseData(master)),
        Collections.<CollapseData>emptyList(),
        author);
  }

  /**
   * Collapse a set of nodes under a specific master node.
   *
   * @param master node to represent collapsed nodes.  Should be in picked.
   * @param picked collection of nodes to collapse
   * @param erase erase (and merge) any collapsed nodes in picked as part of
   * the new collapsed node
   * @param author the initiator for this process
   */
  public void uncollapse(
      GraphNode master, boolean deleteGroup, Object author) {
    CollapseData removedGroup = collapser.getCollapseData(master);
    if (null == removedGroup) {
      return;
    }

    // Perform the collapsing
    collapser.uncollapse(master, deleteGroup);

    fireCollapseChanged(
        Collections.<CollapseData>emptyList(),
        Collections.singleton(removedGroup),
        author);
  }

  /**
   * Collapse all Nodes in the exposed graph using the hierarchy implied
   * by the given set of relations.
   * <p>
   * The algorithm works by computing a topological sort over the imputed
   * hierarchy, and then collapsing the nodes in order from bottom to top.
   * This allows a user to later uncollapse individual masters,
   * and to incrementally expose their internal details.
   *
   * @param graph source of nodes to collapse
   * @param finder set of relations that define the hierarchy
   * @param author interface component that initiated the action
   */
  public void autoCollapse(
      GraphModel graph, DirectedRelationFinder finder, Object author) {
    TreeModel treeData = new TreeModel(
        getExposedGraph(graph).computeSuccessorHierarchy(finder));

    Collection<CollapseData> collapseChanges =
        collapser.collapseTree(graph, treeData);

    fireCollapseChanged(
      collapseChanges,
      Collections.<CollapseData>emptyList(),
      author);
  }

  private void fireCollapseChanged(
      final Collection<CollapseData> created,
      final Collection<CollapseData> removed,
      final Object author) {

    // Ignore empty collapse events - listeners never care
    if (created.isEmpty() && removed.isEmpty()) {
      return;
    }

    listeners.fireEvent(new SimpleDispatcher() {
      @Override
      public void dispatch(ViewPrefsListener listener) {
        listener.collapseChanged(created, removed, author);
      }
    });
  }
}
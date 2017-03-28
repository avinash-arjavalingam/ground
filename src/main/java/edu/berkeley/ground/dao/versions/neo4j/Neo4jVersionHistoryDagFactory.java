/**
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package edu.berkeley.ground.dao.versions.neo4j;

import edu.berkeley.ground.dao.versions.VersionHistoryDagFactory;
import edu.berkeley.ground.db.Neo4jClient;
import edu.berkeley.ground.exceptions.GroundException;
import edu.berkeley.ground.model.versions.Version;
import edu.berkeley.ground.model.versions.VersionHistoryDag;
import edu.berkeley.ground.model.versions.VersionSuccessor;

import java.util.ArrayList;
import java.util.List;

import org.neo4j.driver.v1.types.Relationship;

public class Neo4jVersionHistoryDagFactory extends VersionHistoryDagFactory {
  private final Neo4jClient dbClient;
  private final Neo4jVersionSuccessorFactory versionSuccessorFactory;

  public Neo4jVersionHistoryDagFactory(Neo4jClient dbClient,
                                       Neo4jVersionSuccessorFactory versionSuccessorFactory) {
    this.dbClient = dbClient;
    this.versionSuccessorFactory = versionSuccessorFactory;
  }

  @Override
  public <T extends Version> VersionHistoryDag<T> create(long itemId) throws GroundException {
    return construct(itemId);
  }

  /**
   * Retrieve a DAG from the database.
   *
   * @param itemId the id of the item whose dag we are retrieving
   * @param <T> the type of the versions in this dag
   * @return the retrieved DAG
   * @throws GroundException an error retrieving the DAG
   */
  @Override
  public <T extends Version> VersionHistoryDag<T> retrieveFromDatabase(long itemId)
      throws GroundException {

    List<Relationship> result = this.dbClient.getDescendantEdgesByLabel(itemId, "VersionSuccessor");

    if (result.isEmpty()) {
      // do nothing' this just means that no versions have been added yet.
      return VersionHistoryDagFactory.construct(itemId, new ArrayList<VersionSuccessor<T>>());
    }

    List<VersionSuccessor<T>> edges = new ArrayList<>();

    for (Relationship relationship : result) {
      edges.add(this.versionSuccessorFactory.retrieveFromDatabase(relationship.get("id").asLong()));
    }

    return construct(itemId, edges);
  }

  /**
   * Add an edge to the DAG.
   *
   * @param dag the DAG to update
   * @param parentId the parent's id
   * @param childId the child's id
   * @param itemId the id of the Item whose DAG we're updating
   * @throws GroundException an error adding the edge
   */
  @Override
  public void addEdge(VersionHistoryDag dag, long parentId, long childId, long itemId)
      throws GroundException {

    VersionSuccessor successor = this.versionSuccessorFactory.create(parentId, childId);

    dag.addEdge(parentId, childId, successor.getId());
  }
}
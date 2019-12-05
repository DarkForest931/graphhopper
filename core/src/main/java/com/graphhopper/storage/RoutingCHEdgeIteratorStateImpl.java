/*
 *  Licensed to GraphHopper GmbH under one or more contributor
 *  license agreements. See the NOTICE file distributed with this work for
 *  additional information regarding copyright ownership.
 *
 *  GraphHopper GmbH licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except in
 *  compliance with the License. You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.graphhopper.storage;

import com.graphhopper.routing.weighting.Weighting;
import com.graphhopper.util.CHEdgeIteratorState;
import com.graphhopper.util.EdgeIterator;
import com.graphhopper.util.EdgeIteratorState;

public class RoutingCHEdgeIteratorStateImpl implements RoutingCHEdgeIteratorState {
    private final EdgeIteratorState edgeState;
    private final Weighting weighting;

    public RoutingCHEdgeIteratorStateImpl(EdgeIteratorState edgeState, Weighting weighting) {
        this.edgeState = edgeState;
        this.weighting = weighting;
    }

    @Override
    public EdgeIteratorState getBaseGraphEdgeState() {
        if (isShortcut()) {
            throw new IllegalStateException("Base edge can only be obtained for original edges, was: " + edgeState());
        }
        return edgeState();
    }

    @Override
    public int getEdge() {
        return edgeState().getEdge();
    }

    @Override
    public int getOrigEdgeFirst() {
        return edgeState().getOrigEdgeFirst();
    }

    @Override
    public int getOrigEdgeLast() {
        return edgeState().getOrigEdgeLast();
    }

    @Override
    public int getBaseNode() {
        return edgeState().getBaseNode();
    }

    @Override
    public int getAdjNode() {
        return edgeState().getAdjNode();
    }

    @Override
    public boolean isShortcut() {
        return (edgeState() instanceof CHEdgeIteratorState) && ((CHEdgeIteratorState) edgeState()).isShortcut();
    }

    @Override
    public int getSkippedEdge1() {
        return ((CHEdgeIteratorState) edgeState()).getSkippedEdge1();
    }

    @Override
    public int getSkippedEdge2() {
        return ((CHEdgeIteratorState) edgeState()).getSkippedEdge2();
    }

    @Override
    public double getWeight(boolean reverse) {
        if (isShortcut()) {
            return ((CHEdgeIteratorState) edgeState()).getWeight();
        } else {
            return weighting.calcWeight(getBaseGraphEdgeState(), reverse, EdgeIterator.NO_EDGE);
        }
    }

    EdgeIteratorState edgeState() {
        // use this only via this getter method as it might have been overwritten
        return edgeState;
    }
}

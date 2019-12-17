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

package com.graphhopper.routing.weighting;

import com.graphhopper.routing.util.FlagEncoder;
import com.graphhopper.routing.util.HintsMap;
import com.graphhopper.util.EdgeIteratorState;

public class QueryWeighting implements Weighting {
    private final Weighting weighting;
    private final TurnCostProvider turnCostProvider;

    public QueryWeighting(Weighting weighting, TurnCostProvider turnCostProvider) {
        this.weighting = weighting;
        this.turnCostProvider = turnCostProvider;
    }

    @Override
    public double getMinWeight(double distance) {
        return weighting.getMinWeight(distance);
    }

    @Override
    public double calcEdgeWeight(EdgeIteratorState edgeState, boolean reverse) {
        return weighting.calcEdgeWeight(edgeState, reverse);
    }

    @Override
    public double calcWeight(EdgeIteratorState edgeState, boolean reverse, int prevOrNextEdgeId) {
        return AbstractWeighting.calcWeightWithTurnWeight(weighting, turnCostProvider, edgeState, reverse, prevOrNextEdgeId);
    }

    @Override
    public double calcTurnWeight(int inEdge, int viaNode, int outEdge) {
        return turnCostProvider.calcTurnWeight(inEdge, viaNode, outEdge);
    }

    @Override
    public long calcEdgeMillis(EdgeIteratorState edgeState, boolean reverse) {
        return weighting.calcEdgeMillis(edgeState, reverse);
    }

    @Override
    public long calcMillis(EdgeIteratorState edgeState, boolean reverse, int prevOrNextEdgeId) {
        return AbstractWeighting.calcMillisWithTurnMillis(weighting, turnCostProvider, edgeState, reverse, prevOrNextEdgeId);
    }

    @Override
    public long calcTurnMillis(int inEdge, int viaNode, int outEdge) {
        return turnCostProvider.calcTurnMillis(inEdge, viaNode, outEdge);
    }

    @Override
    public FlagEncoder getFlagEncoder() {
        return weighting.getFlagEncoder();
    }

    @Override
    public String getName() {
        return weighting.getName();
    }

    @Override
    public boolean matches(HintsMap map) {
        return weighting.matches(map);
    }
}

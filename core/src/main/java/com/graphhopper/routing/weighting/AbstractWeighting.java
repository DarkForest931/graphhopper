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

import com.graphhopper.routing.profiles.BooleanEncodedValue;
import com.graphhopper.routing.profiles.DecimalEncodedValue;
import com.graphhopper.routing.util.FlagEncoder;
import com.graphhopper.routing.util.HintsMap;
import com.graphhopper.util.EdgeIterator;
import com.graphhopper.util.EdgeIteratorState;

import static com.graphhopper.util.EdgeIterator.NO_EDGE;
import static com.graphhopper.util.Helper.toLowerCase;

/**
 * @author Peter Karich
 */
public abstract class AbstractWeighting implements Weighting {
    protected final FlagEncoder flagEncoder;
    protected final DecimalEncodedValue avSpeedEnc;
    protected final BooleanEncodedValue accessEnc;
    private final TurnCostProvider turnCostProvider;

    protected AbstractWeighting(FlagEncoder encoder) {
        this(encoder, new NoTurnCostProvider());
    }

    protected AbstractWeighting(FlagEncoder encoder, TurnCostProvider turnCostProvider) {
        this.flagEncoder = encoder;
        if (!flagEncoder.isRegistered())
            throw new IllegalStateException("Make sure you add the FlagEncoder " + flagEncoder + " to an EncodingManager before using it elsewhere");
        if (!isValidName(getName()))
            throw new IllegalStateException("Not a valid name for a Weighting: " + getName());

        avSpeedEnc = encoder.getAverageSpeedEnc();
        accessEnc = encoder.getAccessEnc();
        this.turnCostProvider = turnCostProvider;
    }

    @Override
    public double calcWeight(EdgeIteratorState edgeState, boolean reverse, int prevOrNextEdgeId) {
        return calcWeightWithTurnWeight(this, turnCostProvider, edgeState, reverse, prevOrNextEdgeId);
    }

    public static double calcWeightWithTurnWeight(Weighting weighting, TurnCostProvider turnCostProvider, EdgeIteratorState edgeState, boolean reverse, int prevOrNextEdgeId) {
        final double edgeWeight = weighting.calcEdgeWeight(edgeState, reverse);
        if (!EdgeIterator.Edge.isValid(prevOrNextEdgeId)) {
            return edgeWeight;
        }
        final int origEdgeId = reverse ? edgeState.getOrigEdgeLast() : edgeState.getOrigEdgeFirst();
        double turnWeight = reverse
                ? turnCostProvider.calcTurnWeight(origEdgeId, edgeState.getBaseNode(), prevOrNextEdgeId)
                : turnCostProvider.calcTurnWeight(prevOrNextEdgeId, edgeState.getBaseNode(), origEdgeId);
        return edgeWeight + turnWeight;
    }

    @Override
    public final double calcEdgeWeight(EdgeIteratorState edgeState, boolean reverse) {
        return calcWeight(edgeState, reverse, NO_EDGE);
    }

    @Override
    public final long calcEdgeMillis(EdgeIteratorState edgeState, boolean reverse) {
        return calcMillis(edgeState, reverse, NO_EDGE);
    }

    @Override
    public long calcMillis(EdgeIteratorState edgeState, boolean reverse, int prevOrNextEdgeId) {
        return calcMillisWithTurnMillis(this, turnCostProvider, edgeState, reverse, prevOrNextEdgeId);
    }

    public static long calcMillisWithTurnMillis(Weighting weighting, TurnCostProvider turnCostProvider, EdgeIteratorState edgeState, boolean reverse, int prevOrNextEdgeId) {
        long edgeMillis = weighting.calcEdgeMillis(edgeState, reverse);
        if (!EdgeIterator.Edge.isValid(prevOrNextEdgeId)) {
            return edgeMillis;
        }
        // should we also separate weighting vs. time for turn? E.g. a fast but dangerous turn - is this common?
        // todo: why no first/last orig edge here as in calcWeight ?
        final int origEdgeId = edgeState.getEdge();
        long turnMillis = reverse
                ? turnCostProvider.calcTurnMillis(origEdgeId, edgeState.getBaseNode(), prevOrNextEdgeId)
                : turnCostProvider.calcTurnMillis(prevOrNextEdgeId, edgeState.getBaseNode(), origEdgeId);
        return edgeMillis + turnMillis;
    }

    @Override
    public long calcEdgeMillis(EdgeIteratorState edgeState, boolean reverse) {
        // special case for loop edges: since they do not have a meaningful direction we always need to read them in
        // forward direction
        if (edgeState.getBaseNode() == edgeState.getAdjNode()) {
            reverse = false;
        }

        if (reverse && !edgeState.getReverse(accessEnc) || !reverse && !edgeState.get(accessEnc))
            throw new IllegalStateException("Calculating time should not require to read speed from edge in wrong direction. " +
                    "(" + edgeState.getBaseNode() + " - " + edgeState.getAdjNode() + ") "
                    + edgeState.fetchWayGeometry(3) + ", dist: " + edgeState.getDistance() + " "
                    + "Reverse:" + reverse + ", fwd:" + edgeState.get(accessEnc) + ", bwd:" + edgeState.getReverse(accessEnc) + ", fwd-speed: " + edgeState.get(avSpeedEnc) + ", bwd-speed: " + edgeState.getReverse(avSpeedEnc));

        double speed = reverse ? edgeState.getReverse(avSpeedEnc) : edgeState.get(avSpeedEnc);
        if (Double.isInfinite(speed) || Double.isNaN(speed) || speed < 0)
            throw new IllegalStateException("Invalid speed stored in edge! " + speed);
        if (speed == 0)
            throw new IllegalStateException("Speed cannot be 0 for unblocked edge, use access properties to mark edge blocked! Should only occur for shortest path calculation. See #242.");

        return (long) (edgeState.getDistance() * 3600 / speed);
    }

    @Override
    public double calcTurnWeight(int inEdge, int viaNode, int outEdge) {
        return turnCostProvider.calcTurnWeight(inEdge, viaNode, outEdge);
    }

    @Override
    public long calcTurnMillis(int inEdge, int viaNode, int outEdge) {
        return turnCostProvider.calcTurnMillis(inEdge, viaNode, outEdge);
    }

    @Override
    public boolean matches(HintsMap reqMap) {
        return (reqMap.getWeighting().isEmpty() || getName().equals(reqMap.getWeighting())) &&
                (reqMap.getVehicle().isEmpty() || flagEncoder.toString().equals(reqMap.getVehicle()));
    }

    @Override
    public FlagEncoder getFlagEncoder() {
        return flagEncoder;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 71 * hash + toString().hashCode();
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        final Weighting other = (Weighting) obj;
        return toString().equals(other.toString());
    }

    static boolean isValidName(String name) {
        if (name == null || name.isEmpty())
            return false;

        return name.matches("[\\|_a-z]+");
    }

    /**
     * Replaces all characters which are not numbers, characters or underscores with underscores
     */
    public static String weightingToFileName(Weighting w) {
        return toLowerCase(w.toString()).replaceAll("\\|", "_");
    }

    @Override
    public String toString() {
        return getName() + "|" + flagEncoder;
    }
}

package betterpaths;

import com.evacipated.cardcrawl.modthespire.lib.*;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.map.MapEdge;
import com.megacrit.cardcrawl.map.MapRoomNode;

import java.util.*;
import java.util.function.ToDoubleFunction;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public final class RedundantPaths {
    @SpirePatch2(clz = AbstractDungeon.class, method = "generateMap")
    public static class Patch {
        public static void Postfix()
        {
            remove(AbstractDungeon.map);
        }
    }

    public static void remove(ArrayList<ArrayList<MapRoomNode>> map)
    {
        IntStream.range(0, map.size()).forEach(currentFloorIndex -> {
            ArrayList<MapRoomNode> currentFloor = map.get(currentFloorIndex);
            List<MapRoomNode> nodesFromHighestToLowestPriority = currentFloor
                    .stream()
                    .sorted(Comparator.comparingDouble(getNodeBiasIn(currentFloor)))
                    .collect(Collectors.toList());
            List<MapRoomNode> nodesFromLowestToHighestPriority = currentFloor
                    .stream()
                    .sorted(Comparator.comparingDouble(getNodeBiasIn(currentFloor)).reversed())
                    .collect(Collectors.toList());

            Map<MapRoomNode, MapRoomNode> redundantNodes = new HashMap<>();
            nodesFromLowestToHighestPriority
                    .stream()
                    .filter(MapRoomNode::hasEdges)
                    .forEach(possiblyWorseNode -> nodesFromHighestToLowestPriority
                            .stream()
                            .filter(possiblyBetterNode -> !redundantNodes.containsKey(possiblyBetterNode))
                            .filter(possiblyBetterNode -> offersSameOptions(map, possiblyWorseNode, possiblyBetterNode))
                            .findFirst()
                            .ifPresent(betterNode -> redundantNodes.put(possiblyWorseNode, betterNode))
                    )
            ;

            if (currentFloorIndex == 0) {
                nodesFromLowestToHighestPriority
                        .stream()
                        .filter(MapRoomNode::hasEdges)
                        .forEach(possiblyWorseNode -> nodesFromHighestToLowestPriority
                                .stream()
                                .filter(possiblyBetterNode -> !redundantNodes.containsKey(possiblyBetterNode))
                                .filter(possiblyBetterNode -> isRenderedRedundantBy(map, possiblyWorseNode, possiblyBetterNode))
                                .findFirst()
                                .ifPresent(betterNode -> redundantNodes.put(possiblyWorseNode, betterNode))
                        )
                ;
            }

            redundantNodes.keySet().forEach(RedundantPaths::deleteNodeFromMap);

            if (currentFloorIndex != 0) {
                map.get(currentFloorIndex - 1).forEach(node -> redundantNodes.forEach((toDeleteNode, replacedBy) -> {
                    if (node.isConnectedTo(toDeleteNode)) {
                        node.delEdge(node.getEdgeConnectedTo(toDeleteNode));
                        node.addEdge(new MapEdge(node.x, node.y, node.offsetX, node.offsetY, replacedBy.x, replacedBy.y, replacedBy.offsetX, replacedBy.offsetY, false));
                    }
                }));
            }

            if (map.size() > currentFloorIndex + 1) {
                map.get(currentFloorIndex + 1)
                        .stream()
                        .filter(nextFloorNode -> currentFloor
                                .stream()
                                .noneMatch(currentFloorNode -> currentFloorNode.isConnectedTo(nextFloorNode))
                        )
                        .forEach(RedundantPaths::deleteNodeFromMap)
                ;
            }
        });
    }

    private static void deleteNodeFromMap(MapRoomNode toDeleteNode) {
        toDeleteNode.getEdges().removeAll(toDeleteNode.getEdges());
    }

    private static ToDoubleFunction<MapRoomNode> getNodeBiasIn(ArrayList<MapRoomNode> floor) {
        return node -> Math.abs(node.x + 0.1 - (float) (floor.size() - 1) / 2);
    }

    private static boolean offersSameOptions(ArrayList<ArrayList<MapRoomNode>> map, MapRoomNode a, MapRoomNode b) {
        return isRenderedRedundantBy(map, a, b) && isRenderedRedundantBy(map, b, a);
    }

    private static boolean isRenderedRedundantBy(ArrayList<ArrayList<MapRoomNode>> map, MapRoomNode possiblyWorseNode, MapRoomNode possiblyBetterNode) {
        return possiblyBetterNode.hasEdges()
                && possiblyBetterNode != possiblyWorseNode
                && possiblyBetterNode.room.getClass() == possiblyWorseNode.room.getClass()
                && possiblyWorseNode.getEdges().stream().allMatch(
                possiblyWorseNodeEdge -> possiblyBetterNode.getEdges().stream().anyMatch(
                        possiblyBetterNodeEdge -> {
                            if (possiblyBetterNodeEdge.dstX == possiblyWorseNodeEdge.dstX && possiblyBetterNodeEdge.dstY == possiblyWorseNodeEdge.dstY) {
                                return true;
                            }

                            MapRoomNode possiblyBetterNodeEdgeDst = map.get(possiblyBetterNodeEdge.dstY).get(possiblyBetterNodeEdge.dstX);
                            MapRoomNode possiblyWorseNodeEdgeDst = map.get(possiblyWorseNodeEdge.dstY).get(possiblyWorseNodeEdge.dstX);

                            return isRenderedRedundantBy(map, possiblyWorseNodeEdgeDst, possiblyBetterNodeEdgeDst);
                        }
                )
        );
    }
}
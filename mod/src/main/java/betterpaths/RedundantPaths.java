package betterpaths;

import com.evacipated.cardcrawl.modthespire.lib.*;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.map.MapEdge;
import com.megacrit.cardcrawl.map.MapRoomNode;
import com.megacrit.cardcrawl.rooms.AbstractRoom;

import java.util.*;
import java.util.stream.Collectors;

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
        ArrayList<MapRoomNode> topFloor = map.get(map.size() - 1);
        Map<Class<? extends AbstractRoom>, MapRoomNode> firstNodeByType = topFloor.stream().reduce(
                new HashMap<>(),
                (_firstNodeByType, node) -> {
                    if (node.hasEdges() && !_firstNodeByType.containsKey(node.room.getClass())) {
                        _firstNodeByType.put(node.room.getClass(), node);
                    }
                    return _firstNodeByType;
                },
                (a, b) -> a
        );
        List<MapRoomNode> toDeleteNodes = topFloor.stream().filter(node -> node != firstNodeByType.get(node.room.getClass())).collect(Collectors.toList());

        toDeleteNodes.forEach(toDeleteNode -> toDeleteNode.getEdges().removeAll(toDeleteNode.getEdges()));
        if (map.size() >= 2) {
            map.get(map.size() - 2).forEach(node -> toDeleteNodes.forEach(toDeleteNode -> {
                if (node.isConnectedTo(toDeleteNode)) {
                    node.delEdge(node.getEdgeConnectedTo(toDeleteNode));
                    MapRoomNode newDst = firstNodeByType.get(toDeleteNode.room.getClass());
                    node.addEdge(new MapEdge(node.x, node.y, node.offsetX, node.offsetY, newDst.x, newDst.y, newDst.offsetX, newDst.offsetY, false));
                }
            }));
        }
    }
}
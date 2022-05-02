package betterpaths;

import com.badlogic.gdx.backends.lwjgl.LwjglApplication;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.localization.LocalizedStrings;
import com.megacrit.cardcrawl.map.MapEdge;
import com.megacrit.cardcrawl.map.MapRoomNode;
import com.megacrit.cardcrawl.rooms.AbstractRoom;
import com.megacrit.cardcrawl.rooms.MonsterRoom;
import com.megacrit.cardcrawl.rooms.RestRoom;
import com.megacrit.cardcrawl.rooms.ShopRoom;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;


final class RedundantPathsTest {
    @BeforeAll
    static void startGame() {
        new LwjglApplication(new CardCrawlGame("/"));
        Settings.language = Settings.GameLanguage.ENG;
        CardCrawlGame.languagePack = new LocalizedStrings();
    }

    @Test
    void remove0() {
        ArrayList<ArrayList<MapRoomNode>> map = buildMap(
                buildFloor(buildNode(new RestRoom(), EdgeDirection.Up), buildNode(new RestRoom(), EdgeDirection.UpLeft))
        );
        RedundantPaths.remove(map);
        assertMapEquals(
                buildMap(
                        buildFloor(buildNode(new RestRoom(), EdgeDirection.Up), buildNode(new RestRoom()))
                ),
                map
        );
    }

    @Test
    void remove1() {
        ArrayList<ArrayList<MapRoomNode>> map = buildMap(
                buildFloor(buildNode(new ShopRoom(), EdgeDirection.Up), buildNode(new MonsterRoom(), EdgeDirection.UpLeft)),
                buildFloor(buildNode(new RestRoom(), EdgeDirection.Up), buildNode(new RestRoom(), EdgeDirection.Up))
        );

        RedundantPaths.remove(map);
        assertMapEquals(
                buildMap(
                        buildFloor(buildNode(new ShopRoom(), EdgeDirection.Up), buildNode(new MonsterRoom(), EdgeDirection.UpLeft)),
                        buildFloor(buildNode(new RestRoom(), EdgeDirection.Up), buildNode(new RestRoom(), EdgeDirection.Up))
                ),
                map
        );
    }

    @Test
    void remove2() {
        ArrayList<ArrayList<MapRoomNode>> map = buildMap(
                buildFloor(buildNode(new RestRoom(), EdgeDirection.Up), buildNode(new RestRoom(), EdgeDirection.UpLeft)),
                buildFloor(buildNode(new ShopRoom(), EdgeDirection.Up), buildNode(new MonsterRoom(), EdgeDirection.Up))
        );

        RedundantPaths.remove(map);
        assertMapEquals(
                buildMap(
                        buildFloor(buildNode(new RestRoom(), EdgeDirection.Up), buildNode(null)),
                        buildFloor(buildNode(new ShopRoom(), EdgeDirection.Up), buildNode(new MonsterRoom(), EdgeDirection.UpLeft))
                ),
                map
        );
    }

    @Test
    void remove3() {
        ArrayList<ArrayList<MapRoomNode>> map = buildMap(
                buildFloor(buildNode(new RestRoom()), buildNode(new RestRoom(), EdgeDirection.UpLeft)),
                buildFloor(buildNode(new ShopRoom(), EdgeDirection.UpRight), buildNode(new MonsterRoom(), EdgeDirection.Up))
        );

        RedundantPaths.remove(map);
        assertMapEquals(
                buildMap(
                        buildFloor(buildNode(null), buildNode(new RestRoom(), EdgeDirection.UpLeft)),
                        buildFloor(buildNode(new ShopRoom(), EdgeDirection.UpRight), buildNode(new MonsterRoom(), EdgeDirection.Up))
                ),
                map
        );
    }

    void assertMapEquals(ArrayList<ArrayList<MapRoomNode>> expectedMap, ArrayList<ArrayList<MapRoomNode>> actualMap)
    {
        IntStream.range(0, expectedMap.size()).forEach(floorIndex -> {
            ArrayList<MapRoomNode> expectedFloor = expectedMap.get(floorIndex);
            ArrayList<MapRoomNode> actualFloor = actualMap.get(floorIndex);
            assertEquals(expectedFloor.size(), actualFloor.size());
            IntStream.range(0, expectedFloor.size()).forEach(nodeIndex -> {
                MapRoomNode expectedNode = expectedFloor.get(nodeIndex);
                MapRoomNode actualNode = actualFloor.get(nodeIndex);
                assertEquals(expectedNode.getEdges().size(), actualNode.getEdges().size());
                if (expectedNode.hasEdges()) {
                    assertEquals(expectedNode.getRoom().getClass(), actualNode.getRoom().getClass());
                    IntStream.range(0, expectedNode.getEdges().size()).forEach(edgeIndex -> {
                        MapEdge expectedEdge = expectedNode.getEdges().get(edgeIndex);
                        MapEdge actualEdge = actualNode.getEdges().get(edgeIndex);
                        assertEquals(expectedEdge.dstX, actualEdge.dstX);
                        assertEquals(expectedEdge.dstY, actualEdge.dstY);
                    });
                }
            });
        });
    }

    enum EdgeDirection {
        UpLeft (-1),
        Up (0),
        UpRight (1);

        private final int xVector;

        EdgeDirection(int xVector) {
            this.xVector = xVector;
        }

        public final int getXVector() {
            return xVector;
        }
    }

    private ArrayList<ArrayList<MapRoomNode>> buildMap(Floor... aFloors) {
        List<Floor> floors = Arrays.asList(aFloors);
        Collections.reverse(floors);
        return IntStream.range(0, aFloors.length).boxed()
                .map(floorIndex -> {
                    Node[] floorNodes = floors.get(floorIndex).nodes;
                    return IntStream.range(0, floorNodes.length).boxed()
                            .map(nodeIndex -> {
                                Node node = floorNodes[nodeIndex];
                                return makeMapRoomNode(nodeIndex, floorIndex, node.room, node.edgeDirections);
                            })
                            .collect(Collectors.toCollection(ArrayList::new));
                })
                .collect(Collectors.toCollection(ArrayList::new));
    }

    static private Floor buildFloor(Node... nodes) {
        return new Floor(nodes);
    }

    static class Floor {
        public Node[] nodes;

        public Floor(Node... nodes) {
            this.nodes = nodes;
        }
    }

    static private Node buildNode(AbstractRoom room, EdgeDirection... edgeDirections) {
        return new Node(room, edgeDirections);
    }

    static class Node {
        AbstractRoom room;
        EdgeDirection[] edgeDirections;

        public Node(AbstractRoom room, EdgeDirection... edgeDirections) {
            this.room = room;
            this.edgeDirections = edgeDirections;
        }
    }

    static MapRoomNode makeMapRoomNode(int x, int y, AbstractRoom room, EdgeDirection... edges) {
        MapRoomNode node = new MapRoomNode(x, y);
        node.setRoom(room);
        Arrays.stream(edges).sequential().forEach(edge -> node.addEdge(new MapEdge(node.x, node.y, node.offsetX, node.offsetY, node.x + edge.getXVector(), node.y + 1, node.offsetX, node.offsetY, false)));
        return node;
    }
}
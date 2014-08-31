import java.util.*;
import java.util.function.IntFunction;
import java.util.stream.Collectors;

public class Main {
  static final class Point {
    public final int x, y;
    public Point(int x, int y) { this.x = x; this.y = y; }
  }

  static final class RoverPlacement {
    public final Point pos;
    public final Orientation orient;
    public RoverPlacement(Point pos, Orientation orient) { this.pos = pos; this.orient = orient; }
  }

  static final class Rover {
    public final Map<Orientation, Point> forwardDeltas;
    public final Map<Instruction, Integer> orientDeltas;
    public Rover(Map<Orientation, Point> forwardDeltas, Map<Instruction, Integer> orientDeltas) {
      this.forwardDeltas = Collections.unmodifiableMap(forwardDeltas);
      this.orientDeltas = Collections.unmodifiableMap(orientDeltas);
    }
  }

  static final class World {
    public final Point edge;
    public final Rover rover;
    public final List<Orientation> orientations;
    public World(Point edge, Rover rover, List<Orientation> orientations) {
      this.edge = edge;
      this.rover = rover;
      this.orientations = Collections.unmodifiableList(orientations);
    }
  }

  enum Instruction { LEFT, RIGHT, FORWARD }
  enum Orientation { NORTH, EAST, SOUTH, WEST }

  interface Plateau { boolean Plateau$isValidPosition(World world, Point position); }

  interface MarsPlateau extends Plateau {
    default boolean Plateau$isValidPosition(World world, Point pos) {
      return pos.x <= world.edge.x && pos.y <= world.edge.y && pos.x >= 0 && pos.y >= 0;
    }
  }

  interface PointOps { Point PointOps$sum(Point a, Point b); }

  interface MarsPointOps extends PointOps {
    default Point PointOps$sum(Point a, Point b) {
      return new Point(a.x + b.x, a.y + b.y);
    }
  }

  interface RoverMovement {
    Orientation RoverMovement$orientation(World world, Orientation orient, Instruction instr);
    Point RoverMovement$position(Rover rover, RoverPlacement placement, Instruction instr);
  }

  interface MarsRoverMovement extends RoverMovement, PointOps {
    default Orientation RoverMovement$orientation(World world, Orientation orient, Instruction instr) {
      final int delta = world.rover.orientDeltas.get(instr);
      final int orientCount = world.orientations.size();
      return world.orientations.get((orientCount + (world.orientations.indexOf(orient) + delta)) % orientCount);
    }

    default Point RoverMovement$position(Rover rover, RoverPlacement placement, Instruction instr) {
      if (instr == Instruction.FORWARD) {
        return PointOps$sum(placement.pos, rover.forwardDeltas.get(placement.orient));
      }
      return placement.pos;
    }
  }

  interface RoverInstructions {
    List<Instruction> RoverInstructions$parseInstructions(String instructionLine);
    RoverPlacement RoverInstructions$parsePlacement(String placementLine);
  }

  interface MarsRoverInstructions extends RoverInstructions {
    default RoverPlacement RoverInstructions$parsePlacement(String placementLine) {
      final IntFunction<Orientation> toOrientation = o -> {
        switch(o) {
          case 'N': return Orientation.NORTH;
          case 'E': return Orientation.EAST;
          case 'S': return Orientation.SOUTH;
          case 'W': return Orientation.WEST;
        }
        throw new IllegalArgumentException("argument out of range: " + o);
      };
      final List<String> startLineSplit = Arrays.asList(placementLine.split(" "));
      final Point pos = new Point(Integer.parseInt(startLineSplit.get(0)), Integer.parseInt(startLineSplit.get(1)));
      final Orientation orient = toOrientation.apply(startLineSplit.get(2).charAt(0));
      return new RoverPlacement(pos, orient);
    }

    default List<Instruction> RoverInstructions$parseInstructions(String instructionLine) {
      final IntFunction<Instruction> toInstruction = instr -> {
        switch(instr) {
          case 'L': return Instruction.LEFT;
          case 'R': return Instruction.RIGHT;
          case 'M': return Instruction.FORWARD;
        }
        throw new IllegalArgumentException("argument out of range: " + instr);
      };
      return Collections.unmodifiableList(instructionLine.chars().mapToObj(toInstruction).collect(Collectors.toList()));
    }
  }

  interface Mars extends RoverInstructions, RoverMovement, Plateau {
    default RoverPlacement navigate(World world, String placementLine, String instructionLine) {
      return RoverInstructions$parseInstructions(instructionLine).stream().reduce(
          RoverInstructions$parsePlacement(placementLine),
          (plcment, instr) -> applyInstruction(world, plcment, instr),
          (a, b) -> b);
    }

    default RoverPlacement applyInstruction(World world, RoverPlacement state, Instruction instruction) {
      final Orientation orient = RoverMovement$orientation(world, state.orient, instruction);
      final Point pos = RoverMovement$position(world.rover, state, instruction);
      if (Plateau$isValidPosition(world, pos)) {
        return new RoverPlacement(pos, orient);
      } else {
        throw new IllegalStateException("Invalid move: " + instruction);
      }
    }
  }

  interface RoverMovementWithPointOps extends MarsRoverMovement, MarsPointOps {}
  static class MarsModule implements Mars, MarsPlateau, MarsRoverInstructions, RoverMovementWithPointOps {}

  public static void main(String[] args) {
    Map<Orientation, Point> forwardDeltas = new HashMap<>();
    forwardDeltas.put(Orientation.NORTH, new Point(0, 1));
    forwardDeltas.put(Orientation.EAST, new Point(1, 0));
    forwardDeltas.put(Orientation.SOUTH, new Point(0, -1));
    forwardDeltas.put(Orientation.WEST, new Point(-1, 0));

    Map<Instruction, Integer> orientDeltas = new HashMap<>();
    orientDeltas.put(Instruction.LEFT, -1);
    orientDeltas.put(Instruction.RIGHT, 1);
    orientDeltas.put(Instruction.FORWARD, 0);

    final Rover rover = new Rover(forwardDeltas, orientDeltas);

    final List<Orientation> orientOrder =
        Arrays.asList(Orientation.NORTH, Orientation.EAST, Orientation.SOUTH, Orientation.WEST);

    final World world = new World(new Point(5, 5), rover, orientOrder);

    final RoverPlacement result = new MarsModule().navigate(world, "1 2 N", "LMLMLMLMM");

    System.out.println(result.pos.x);
    System.out.println(result.pos.y);
    System.out.println(result.orient);

    final RoverPlacement result2 = new MarsModule().navigate(world, "3 3 E", "MMRMMRMRRM");

    System.out.println(result2.pos.x);
    System.out.println(result2.pos.y);
    System.out.println(result2.orient);
  }
}

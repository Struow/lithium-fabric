package me.jellysquid.mods.lithium.common.shapes;

import com.google.common.collect.Lists;
import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import it.unimi.dsi.fastutil.doubles.DoubleList;
import net.minecraft.util.math.AxisCycleDirection;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelSet;
import net.minecraft.util.shape.VoxelShape;

import java.util.List;

/**
 * An efficient implementation of {@link VoxelShape} for a shape with one simple cuboid. Since there are only ever two
 * vertices in a single cuboid (the start and end points), we can eliminate needing to iterate over voxels and to find
 * vertices through using simple comparison logic to pick between either the start or end point.
 * <p>
 * Additionally, the function responsible for determining shape penetration has been simplified and optimized by taking
 * advantage of the fact that there is only ever one voxel in a simple cuboid shape, greatly speeding up collision
 * handling in most cases as block shapes are often nothing more than a single cuboid.
 */
public class VoxelShapeSimpleCube extends VoxelShape implements VoxelShapeExtended {
    private static final double EPSILON = 1.0E-7D;

    private final double x1, y1, z1, x2, y2, z2;

    public VoxelShapeSimpleCube(VoxelSet voxels, double x1, double y1, double z1, double x2, double y2, double z2) {
        super(voxels);

        this.x1 = x1;
        this.y1 = y1;
        this.z1 = z1;
        this.x2 = x2;
        this.y2 = y2;
        this.z2 = z2;
    }

    @Override
    public VoxelShape offset(double x, double y, double z) {
        return new VoxelShapeSimpleCube(this.voxels, this.x1 + x, this.y1 + y, this.z1 + z, this.x2 + x, this.y2 + y, this.z2 + z);
    }

    @Override
    public double calculateMaxDistance(AxisCycleDirection cycleDirection, Box box, double maxDist) {
        if (Math.abs(maxDist) < EPSILON) {
            return 0.0D;
        }

        double penetration = this.calculatePenetration(cycleDirection, box, maxDist);

        if ((penetration != maxDist) && this.intersects(cycleDirection, box)) {
            return penetration;
        }

        return maxDist;
    }

    private double calculatePenetration(AxisCycleDirection dir, Box box, double maxDist) {
        switch (dir) {
            case NONE:
                return this.calculatePenetration(this.x1, this.x2, box.minX, box.maxX, maxDist);
            case FORWARD:
                return this.calculatePenetration(this.z1, this.z2, box.minZ, box.maxZ, maxDist);
            case BACKWARD:
                return this.calculatePenetration(this.y1, this.y2, box.minY, box.maxY, maxDist);
            default:
                throw new IllegalArgumentException();
        }
    }

    private boolean intersects(AxisCycleDirection dir, Box box) {
        switch (dir) {
            case NONE:
                return lessThan(this.y1, box.maxY) && lessThan(box.minY, this.y2) && lessThan(this.z1, box.maxZ) && lessThan(box.minZ, this.z2);
            case FORWARD:
                return lessThan(this.x1, box.maxX) && lessThan(box.minX, this.x2) && lessThan(this.y1, box.maxY) && lessThan(box.minY, this.y2);
            case BACKWARD:
                return lessThan(this.z1, box.maxZ) && lessThan(box.minZ, this.z2) && lessThan(this.x1, box.maxX) && lessThan(box.minX, this.x2);
            default:
                throw new IllegalArgumentException();
        }
    }

    private double calculatePenetration(double a1, double a2, double b1, double b2, double maxDist) {
        double penetration;

        if (maxDist > 0.0D) {
            penetration = a1 - b2;

            if ((penetration < -EPSILON) || (maxDist < penetration)) {
                return maxDist;
            }

            if (penetration < EPSILON) {
                return 0.0D;
            }
        } else {
            penetration = a2 - b1;

            if ((penetration > EPSILON) || (maxDist > penetration)) {
                return maxDist;
            }

            if (penetration > -EPSILON) {
                return 0.0D;
            }
        }

        return penetration;
    }

    @Override
    public List<Box> getBoundingBoxes() {
        return Lists.newArrayList(this.getBoundingBox());
    }

    @Override
    public Box getBoundingBox() {
        return new Box(this.x1, this.y1, this.z1, this.x2, this.y2, this.z2);
    }

    @Override
    public double getMin(Direction.Axis axis) {
        return axis.choose(this.x1, this.y1, this.z1);
    }

    @Override
    public double getMax(Direction.Axis axis) {
        return axis.choose(this.x2, this.y2, this.z2);
    }

    @Override
    protected double getPointPosition(Direction.Axis axis, int index) {
        if ((index < 0) || (index > 1)) {
            throw new ArrayIndexOutOfBoundsException();
        }

        switch (axis) {
            case X:
                return (index == 0) ? this.x1 : this.x2;
            case Y:
                return (index == 0) ? this.y1 : this.y2;
            case Z:
                return (index == 0) ? this.z1 : this.z2;
        }

        throw new IllegalArgumentException();
    }

    @Override
    protected DoubleList getPointPositions(Direction.Axis axis) {
        switch (axis) {
            case X:
                return DoubleArrayList.wrap(new double[]{this.x1, this.x2});
            case Y:
                return DoubleArrayList.wrap(new double[]{this.y1, this.y2});
            case Z:
                return DoubleArrayList.wrap(new double[]{this.z1, this.z2});
        }

        throw new IllegalArgumentException();
    }

    @Override
    protected boolean contains(double x, double y, double z) {
        return (x >= this.x1) && (x < this.x2) && (y >= this.y1) && (y < this.y2) && (z >= this.z1) && (z < this.z2);
    }

    @Override
    public boolean isEmpty() {
        return ((this.x1 + EPSILON) > this.x2) || ((this.y1 + EPSILON) > this.y2) || ((this.z1 + EPSILON) > this.z2);
    }

    @Override
    protected int getCoordIndex(Direction.Axis axis, double coord) {
        if (coord < this.getMin(axis)) {
            return -1;
        }

        if (coord >= this.getMax(axis)) {
            return 1;
        }

        return 0;
    }

    private static boolean lessThan(double a, double b) {
        return (a + EPSILON) < b;
    }

    @Override
    public boolean intersects(Box box, double x, double y, double z) {
        return (box.minX < (this.x2 + x)) && (box.maxX > (this.x1 + x)) &&
                (box.minY < (this.y2 + y)) && (box.maxY > (this.y1 + y)) &&
                (box.minZ < (this.z2 + z)) && (box.maxZ > (this.z1 + z));
    }
}

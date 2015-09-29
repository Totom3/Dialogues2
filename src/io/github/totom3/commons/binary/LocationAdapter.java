package io.github.totom3.commons.binary;

import java.io.IOException;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

/**
 *
 * @author Totom3
 */
public class LocationAdapter implements BinaryAdapter<Location> {

    static byte NO_X = 0x1;
    static byte NO_Y = 0x2;
    static byte NO_Z = 0x4;
    static byte NO_PITCH = 0x08;
    static byte NO_YAW = 0x10;

    @Override
    public Location read(DeserializationContext context) throws IOException {
	String worldName = context.readString();

	World world = null;
	if (worldName != null) {
	    world = Bukkit.getWorld(worldName);
	    if (world == null) {
		throw new DeserializingException("No such world " + worldName);
	    }
	}

	byte header = context.readByte();

	double x = 0, y = 0, z = 0;
	float pitch = 0, yaw = 0;

	if ((header & NO_X) == 0) {
	    x = context.readDouble();
	}
	if ((header & NO_Y) == 0) {
	    y = context.readDouble();
	}
	if ((header & NO_Z) == 0) {
	    z = context.readDouble();
	}
	if ((header & NO_PITCH) == 0) {
	    pitch = context.readFloat();
	}
	if ((header & NO_YAW) == 0) {
	    yaw = context.readFloat();
	}

	return new Location(world, x, y, z, yaw, pitch);
    }

    @Override
    public void write(Location loc, SerializationContext context) throws IOException {
	String worldName = (loc.getWorld() == null) ? null : loc.getWorld().getName();
	context.writeString(worldName);

	double x = loc.getX(), y = loc.getY(), z = loc.getZ();
	float yaw = loc.getYaw(), pitch = loc.getPitch();

	byte header = makeHeader(x, y, z, pitch, yaw);
	context.writeByte(header);

	if ((header & NO_X) == 0) {
	    context.writeDouble(x);
	}
	if ((header & NO_Y) == 0) {
	    context.writeDouble(y);
	}
	if ((header & NO_Z) == 0) {
	    context.writeDouble(z);
	}
	if ((header & NO_PITCH) == 0) {
	    context.writeFloat(pitch);
	}
	if ((header & NO_YAW) == 0) {
	    context.writeFloat(yaw);
	}
    }

    protected byte makeHeader(double x, double y, double z, float pitch, float yaw) {
	byte head = 0;

	if (x == 0) {
	    head |= NO_X;
	}
	if (y == 0) {
	    head |= NO_Y;
	}
	if (z == 0) {
	    head |= NO_Z;
	}
	if (pitch == 0) {
	    head |= NO_PITCH;
	}
	if (yaw == 0) {
	    head |= NO_YAW;
	}

	return head;
    }
}

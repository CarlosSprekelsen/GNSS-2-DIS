
package ets.acmi.gnssdislogger.common;


import android.location.Location;

import edu.nps.moves.dis.Vector3Double;
import edu.nps.moves.dis.Vector3Float;


public class Maths {

    public static double RADIANS_TO_DEGREES = 180.0 / Math.PI;
    public static double DEGREES_TO_RADIANS = Math.PI / 180.0;

    private static int P_B= 227;
    private static int P_M = 1000005;
    /**
     * Uses the Haversine formula to calculate the distance (meters) between two lat-long coordinates
     *
     * @param latitude1  The first point's latitude
     * @param longitude1 The first point's longitude
     * @param latitude2  The second point's latitude
     * @param longitude2 The second point's longitude
     * @return The distance between the two points in meters
     */
    public static double calculateDistance(double latitude1, double longitude1, double latitude2, double longitude2) {
        /*
            Haversine formula:
            A = sin²(Δlat/2) + cos(lat1).cos(lat2).sin²(Δlong/2)
            C = 2.atan2(√a, √(1−a))
            D = R.c
            R = radius of earth, 6371 km.
            All angles are in radians
            */

        double deltaLatitude = Math.toRadians(Math.abs(latitude1 - latitude2));
        double deltaLongitude = Math.toRadians(Math.abs(longitude1 - longitude2));
        double latitude1Rad = Math.toRadians(latitude1);
        double latitude2Rad = Math.toRadians(latitude2);

        double a = Math.pow(Math.sin(deltaLatitude / 2), 2) +
                (Math.cos(latitude1Rad) * Math.cos(latitude2Rad) * Math.pow(Math.sin(deltaLongitude / 2), 2));

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return 6371 * c * 1000; //Distance in meters

    }

    /**
     * Converts given meters/second to nautical mile/hour.
     *
     * @param mps meters per second
     * @return knots
     */
    public static double mpsToKnots(double mps) {
        // Google "meters per second to knots"
        return mps * 1.94384449;
    }

    /**
     * Checks bundle in the Location object for satellties used in fix.
     *
     * @param loc The location object to query
     * @return satellites used in fix, or 0 if no value found.
     */
    public static int getBundledSatelliteCount(Location loc) {
        int sat = 0;

        if (loc.getExtras() != null) {
            sat = loc.getExtras().getInt("satellites", 0);

            if (sat == 0) {
                //Provider gave us nothing, let's look at our bundled count
                sat = loc.getExtras().getInt(BundleConstants.SATELLITES_FIX, 0);
            }
        }

        return sat;
    }


    /**
     * Gets Yaw,Pitch and Roll from two ECEF XYZ locations
     * At first we subtract vector one from vector two in order to get vector two relative to vector one.
     * With these values you can calculate Euler angles.
     * To understand the calculation from vector to Euler intuitively, lets imagine a sphere with the radius of 1 and the origin at its center.
     * A vector represents a point on its surface in 3D coordinates. This point can also be defined by spherical 2D coordinates: latitude and longitude, pitch and yaw respectively.
     *
     * In Euler the order of your axes matters, mix them up and you will get different results
     * In order "roll <- pitch <- yaw" calculation can be done as follows:
     * To calculate the yaw you calculate the tangent of the two planar axes (x and z) considering the quadrant.
     * yaw = atan2(x, z) *180.0/PI;
     * Pitch is quite the same but as its plane is rotated along with yaw the 'adjacent' is on two axis. In order to find its length we will have to use the Pythagorean theorem.
     *   float padj = sqrt(pow(x, 2) + pow(z, 2));
     *   pitch = atan2(padj, y) *180.0/PI;
     * Roll can not be calculated as a vector has no rotation around its own axis. I usually set it to 0
     */
    public static Vector3Double getYawPitchRollfromECEF(Vector3Double start,Vector3Double end){
        double X = end.getX() - start.getX();
        double Y = end.getY() - start.getY();
        double Z = end.getZ() - start.getZ();
        double Yaw_rad = Math.atan2(X,Z);
        double padj = Math.sqrt(Math.pow(X, 2) + Math.pow(Z, 2));
        double Pitch_rad = Math.atan2(padj, Y);
        double Roll_rad =0;
        Vector3Double euler = new Vector3Double();
        euler.setX(Yaw_rad);
        euler.setY(Pitch_rad);
        euler.setZ(Roll_rad);
        return euler;
    }


    public static Vector3Float getECEFfromENU(double lat, double lon, double E, double N, double U){
        double sinLat = Math.sin(lat);
        double cosLat = Math.cos(lat);
        double sinLon = Math.sin(lon);
        double cosLon = Math.cos(lon);
        Vector3Float result = new Vector3Float();
        result.setX( (float) (sinLat*E -cosLat*sinLon*N +cosLat*cosLon*U));
        result.setY( (float)(cosLat*E -sinLat*sinLon*N +sinLat*cosLon*U));
        result.setZ( (float)(cosLon*N + sinLon*U));
        return result;
    }

    public static Vector3Float getVelocityXYZ(Vector3Double currentLocationXYZ, double currentSeconds, Vector3Double lastLocationXYZ, double lastSeconds) {
        double dX = currentLocationXYZ.getX() - lastLocationXYZ.getX();
        double dY = currentLocationXYZ.getY() - lastLocationXYZ.getY();
        double dZ = currentLocationXYZ.getZ() - lastLocationXYZ.getZ();
        double dt = currentSeconds - lastSeconds;
        Vector3Float Velocity = new Vector3Float();
        Velocity.setX((float) (dX / dt));
        Velocity.setY((float) (dY / dt));
        Velocity.setZ((float) (dZ / dt));
        return Velocity;
    }

    public static Vector3Float getAccelerationXYZ(Vector3Float currentVelocityXYZ, double currentSeconds, Vector3Float lastVelocityXYZ, double lastSeconds) {
        double dX = currentVelocityXYZ.getX() - lastVelocityXYZ.getX();
        double dY = currentVelocityXYZ.getY() - lastVelocityXYZ.getY();
        double dZ = currentVelocityXYZ.getZ() - lastVelocityXYZ.getZ();
        double dt = currentSeconds - lastSeconds;
        Vector3Float Acceleration = new Vector3Float();
        Acceleration.setX((float) (dX / dt));
        Acceleration.setY((float) (dY / dt));
        Acceleration.setZ((float) (dZ / dt));
        return Acceleration;
    }



    public static String hash(String s) {

        int r = 0;
        for (int i = 0; i < s.length(); i++) {
            r = r* P_B + s.charAt(i);
            r %= P_M;
        }
        return Integer.toString(r);
    }
}

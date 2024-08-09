package ets.acmi.gnssdislogger.loggers.ieee1278;

import android.location.Location;

import com.birbit.android.jobqueue.JobManager;

import edu.nps.moves.dis.DeadReckoningParameter;
import edu.nps.moves.dis.EntityID;
import edu.nps.moves.dis.EntityStatePdu;
import edu.nps.moves.dis.EntityType;
import edu.nps.moves.dis.Marking;
import edu.nps.moves.dis.Orientation;
import edu.nps.moves.dis.Vector3Double;
import edu.nps.moves.dis.Vector3Float;
import edu.nps.moves.disutil.CoordinateConversions;
import edu.nps.moves.disutil.EulerConversions;
import ets.acmi.gnssdislogger.common.AppSettings;
import ets.acmi.gnssdislogger.common.Maths;
import ets.acmi.gnssdislogger.common.PreferenceHelper;
import ets.acmi.gnssdislogger.common.Session;
import ets.acmi.gnssdislogger.loggers.FileLogger;


public class DisLogger implements FileLogger {

    private static final PreferenceHelper preferenceHelper = PreferenceHelper.getInstance();
    private final String name = "DIS";
    //Constructor

    public DisLogger() {

    }

    @Override
    public void write(Location loc) throws Exception {
        String server = preferenceHelper.getDISServer();
        int port = Integer.parseInt(preferenceHelper.getDISServerPort());
        byte[] buffer = setEntityStatePdu(loc);
        JobManager jobManager = AppSettings.getJobManager();
        jobManager.addJobInBackground(new DisUdpJob(server, port, buffer));
    }

    @Override
    public void annotate(String description, Location loc) throws Exception {
        write(loc);
    }


    @Override
    public String getName() {
        return name;
    }


    /**
     * Encode a location in an EntityStatePDU.
     * <p/>
     * For details check https://github.com/open-dis/open-dis-java
     * (OpenDIS source)
     */
    private byte[] setEntityStatePdu(Location androidlocation) {
        Session session = Session.getInstance();
        EntityStatePdu currentEntityStatePdu = new EntityStatePdu();

        currentEntityStatePdu.setForceId((short) Integer.parseInt(preferenceHelper.getDISForceid()));
        currentEntityStatePdu.setExerciseID((short) Integer.parseInt(preferenceHelper.getDISExercise()));
        EntityID entityID = new EntityID();
        entityID.setSite(Integer.parseInt(preferenceHelper.getDISSite()));
        entityID.setEntity(Integer.parseInt(preferenceHelper.getDISEntityid()));
        entityID.setApplication(Integer.parseInt(preferenceHelper.getDISApplication()));
        currentEntityStatePdu.setEntityID(entityID);
        EntityType entityType = new EntityType();
        String[] enumeration = preferenceHelper.getDISEnumeration().split(":");
        //SISO enumeration example used on preference helper 1.2.225.20.01.01.AH-64 Apache
        //https://www.sisostds.org
        entityType.setEntityKind((short) Integer.parseInt(enumeration[0]));      // Platform (vs lifeform, munition, sensor, etc.)
        entityType.setDomain((short) Integer.parseInt(enumeration[1]));          // Domain (Land, air, surface, subsurface, space)
        entityType.setCountry(Integer.parseInt(enumeration[2]));                 // Country
        entityType.setCategory((short) Integer.parseInt(enumeration[3]));        // Platform
        entityType.setSubcategory((short) Integer.parseInt(enumeration[4]));     // platform type
        entityType.setSpec((short) Integer.parseInt(enumeration[5]));            // Specific
        currentEntityStatePdu.setEntityType(entityType);

        Marking entityMarking = new Marking();
        entityMarking.setCharacterSet((short) 1);
        entityMarking.setCharacters(preferenceHelper.getDISMarking().getBytes());
        currentEntityStatePdu.setMarking(entityMarking);


        // Calculate Current XYZ Location to update position on PDU
        double newLatRadians = androidlocation.getLatitude() * Maths.DEGREES_TO_RADIANS;
        double newLonRadians = androidlocation.getLongitude() * Maths.DEGREES_TO_RADIANS;
        double newAlt = androidlocation.getAltitude();

        double[] newCoordinates = CoordinateConversions.getXYZfromLatLonRadians(newLatRadians, newLonRadians, newAlt);
        Vector3Double newlocXYZ = new Vector3Double();
        newlocXYZ.setX(newCoordinates[0]);
        newlocXYZ.setY(newCoordinates[1]);
        newlocXYZ.setZ(newCoordinates[2]);
        currentEntityStatePdu.setEntityLocation(newlocXYZ);

        //Returns the UTC time of this fix, in milliseconds since GPS EPOCH in January 1, 1970.
        // Calculate the ENU velocity vector with speed and bearing.
        double velocityNorth= androidlocation.getSpeed()*Math.sin(androidlocation.getBearing()*Maths.DEGREES_TO_RADIANS);
        double velocityEast= androidlocation.getSpeed()*Math.sin(androidlocation.getBearing()*Maths.DEGREES_TO_RADIANS);
        Vector3Float newVelocity =   Maths.getECEFfromENU(androidlocation.getLatitude(),androidlocation.getLongitude(),velocityEast,velocityNorth,0);


        double currentSeconds = androidlocation.getTime() * 0.001;
        /*
        double prevSeconds = session.getPreviousSeconds();
        Vector3Double prevLocationXYZ = session.getPreviousEntityStatePdu().getEntityLocation();
        Vector3Float newVelocity = Maths.getVelocityXYZ(newlocXYZ, currentSeconds, prevLocationXYZ, prevSeconds);
         */
        currentEntityStatePdu.setEntityLinearVelocity(newVelocity);

        //Vector3Double euler = Maths.getYawPitchRollfromECEF(prevLocationXYZ,newlocXYZ);
        double Yaw_deg   = androidlocation.getBearing(); //euler.getX() * Maths.RADIANS_TO_DEGREES;
        double Pitch_deg = 0; //euler.getY() * Maths.RADIANS_TO_DEGREES;


        Orientation orientation = currentEntityStatePdu.getEntityOrientation();

        orientation.setTheta((float) EulerConversions.getThetaFromTaitBryanAngles(newLatRadians, newLonRadians, Yaw_deg, Pitch_deg));
        orientation.setPsi((float) EulerConversions.getPsiFromTaitBryanAngles(newLatRadians, newLonRadians, Yaw_deg, Pitch_deg));
        orientation.setPhi((float) EulerConversions.getPhiFromTaitBryanAngles(newLatRadians, newLonRadians, Yaw_deg, Pitch_deg, 0));
/*
        DeadReckoningParameter deadReckoning = new DeadReckoningParameter();
        Vector3Float prevVelocity = session.getPreviousEntityStatePdu().getEntityLinearVelocity();
        Vector3Float newAcceleration = Maths.getAccelerationXYZ(newVelocity, currentSeconds, prevVelocity, prevSeconds);
        deadReckoning.setEntityLinearAcceleration(newAcceleration);
        deadReckoning.setDeadReckoningAlgorithm((short) 5); //DIS_DR_FVW_05
*/

        session.setPreviousEntityStatePdu(currentEntityStatePdu);
        session.setPreviousSeconds(currentSeconds);

        // Marshal out the object to a byte array
        return currentEntityStatePdu.marshalWithUnixTimestamp();
    }


}
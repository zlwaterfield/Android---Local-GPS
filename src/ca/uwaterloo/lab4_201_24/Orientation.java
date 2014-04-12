package ca.uwaterloo.lab4_201_24;

import java.util.ArrayList;
import java.util.List;

import mapper.InterceptPoint;
import mapper.LineSegment;
import mapper.MapView;
import mapper.NavigationalMap;
import mapper.VectorUtils;
import android.graphics.Color;
import android.graphics.PointF;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorManager;
import android.util.Log;
import android.widget.TextView;

public class Orientation {
	
		private SensorListener Accels, Magn, LinearAccels;
		private TextView orient, distanceTxt, distanceTxt2;
		private float[] geoMag, gravity, orientation = new float[3];
		private float[] distance = new float[2];
		private float[] directions = new float[2];
		private float checkSteps = 0;
		private float stepsC = 0;
		MapView mv;
		NavigationalMap n = new NavigationalMap();
		TextView instructions;
		float direction;
		float directionFactor;
		
		//Constructor takes in 3 sensorlisteners and an output view
		public Orientation(SensorListener A, SensorListener M, SensorListener LA, TextView output)
		{
			//create local references to the sensorlisteners in MainActivity
			Accels = A;
			Magn = M;
			LinearAccels = LA;
			
			//Set the orientation of these SensorListeners to this instance, to ensure a connection between the sensorlisteners and orient ahs been established
			Accels.setO(this); 
			Magn.setO(this);
			LinearAccels.setO(this);
			
			//reference local orient to textview output to alow refernce updating
			orient = output;
			//using the Accelerometer we get the acceleration values to create a local copy of them (thus using clone())
			orientation = A.getValues().clone();
		}
		
		public void setMap(NavigationalMap map) 
		{
			n = map;
		}
		
		public void setMapview(MapView mapV) 
		{
			mv = mapV;
		}
		
		//Method to assign the distance of the north and east walking patters based on the orientation angle and the step distance (0.75m)
		public void addDistance()
		{
			
			direction = orientation[0];
			
			directions[0] = (float) Math.cos(direction);
			directions[1] = (float) Math.sin(direction);
		}
		
		//Used to get the orientation array 
		public float[] getOrientValues() {
			return orientation;
		}
		
		//used to get reference to the orient textview for updating in Main
		public TextView getOrientationView() {
			return orient;
		}
		
		//Zero's any distance already traveled in North and East direction
		public void zeroDistance(){
			 distance[0] = 0;
			 distance[1] = 0;	
		}
		
		public void setInstructions(TextView instructions) 
		{
			this.instructions = instructions;
		}
		
		public void finishInstructions(TextView distanceT) 
		{
			this.distanceTxt = distanceT;
		}
		public void finishI(TextView distanceT2) 
		{
			this.distanceTxt2 = distanceT2;
		}
		
		//Taking in a sensor event se this method runs conncurently to onsensorChanged in sensor listener to get the acceleration and/or magnetic values
		public void setValue(SensorEvent se)
		{
			if(se.sensor.getType() == Sensor.TYPE_ACCELEROMETER)//When called by Accelerometer sensor
            {
				gravity = se.values.clone();//don't assign a reference but a local copy
				
            }
			if(se.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD)//when called by Magnetic sensor
            {
				geoMag = se.values.clone();//don't assign a reference but a local copy
            }
			
			if(usable())//Ensures this method is not run unless condition satisfied
			{
				setOrient();//run setOrient to update the values
			}
		}
		 
		//Used to ensure that gravity and magnetic arrays are usable and that the instance is not null
		public boolean usable()
		{
			boolean usable = false;
			if(this != null){//ensure instance has a value
				
				if(gravity != null && geoMag != null)//ensure gravity are not null but are instantiated
				{
					if(gravity.length != 0 && geoMag.length != 0)//ensure gravity and magnetic have been given values already
					{
						usable = true;
					}
				}
			}
			return usable;
		}

		public void setOrient()
		{
			//Two memory alocations for R and I to used in getRotationalMatrix
	 		float[] R = new float[9];
	 		float[] I = new float[9];
	 		
	 		distanceTxt.setText(String.format("NORTH: (%.1f) EAST: (%.1f)",distance[0], distance[1]));
	 		
	 	if(usable())//Check all conditions
	  	 {
	 		
	 		if(SensorManager.getRotationMatrix(R, I, gravity, geoMag))//returns true if r and I have been successfully filled
	 		{
	 			stepsC = LinearAccels.getLAccelSteps();//get the steps currently from Lienar acelerometer sensor
	 			SensorManager.getOrientation(R, orientation );//Uses the now filled R and orientation to return 
	 														//azimuth, pitch and roll in orientation [0,1,3] respectively
	
	 			if(checkSteps < stepsC)//check if a step has been made inLinear Accelerometer
		 			{
		 				this.addDistance();//calls the addDistance to add to the distances of North and East
		 				checkSteps = stepsC;
		 				PointF a = new PointF(mv.getUserPoint().x, mv.getUserPoint().y);
		 				PointF b = new PointF(mv.getUserPoint().x + (directions[0]/2.5f), 
		 										mv.getUserPoint().y + (directions[1]/2.5f));
		 				
		 				//calls canMove Method
		 				if(canMove(a, b))
		 				{
		 					mv.setUserPoint(b);
		 					mv.setUserPath(createUserPath(b, mv.getDestinationPoint()));
		 					//checks if finshed then output text
							if(finish())
		 					{
		 						distanceTxt2.setText("WOW GOOD JOB! YOU CAN FOLLOW A PREDEFINE PATH THAT I " +
		 								"WORKED TIRLESSLY ON PRODUCING, SO THAT YOU KNOW HOW TO MANUEVER " +
		 								"AROUND TABLES AND CHAIRS WITHOUT BREAKING YOUR SHINS D=<");
		 					}
							else{
								distanceTxt2.setText("");
							}
		 					distanceTxt2.setTextColor(Color.MAGENTA);//color
		 				}
		 				checkInstructions(a, b);
		 			}
	 			} 
	  	 	}
		}
		
		//checks cases to output text
		private void checkInstructions(PointF start, PointF end) 
		{
			
			String a = "", b = "", c = "";
			
			//makes sures user has picked starting and destination position
			if(mv.getDestinationPoint().x == 0 && mv.getDestinationPoint().y == 0 ||
					mv.getOriginPoint().x == 0 && mv.getOriginPoint().y == 0)
			{
				a = "\n -You must select a destination and starting point";
			}
			//makes sure starting and destination point arent the same
			if(VectorUtils.areEqual(mv.getDestinationPoint(), mv.getOriginPoint()))
			{
				b = "\n -Try selcting your points again";
			}
			//lets user know if they hit a wall
			if(n.calculateIntersections(start, end).size() != 0 && start.x != 0 && start.y != 0)
			{
				c = "\n -You hit a wall and cannot go this way";
			}
			//outputs text
			instructions.setText(a + b + c + String.format("\n\nCurrently North: (%.2f)",direction));
		}
		
		public void resetPath() 
		{
			PointF a = new PointF(mv.getUserPoint().x, mv.getUserPoint().y);
			mv.setUserPath(createUserPath(a, mv.getDestinationPoint()));
		}
		
		//checks cases to see if the point can move
		public boolean canMove(PointF a, PointF b)
		{
			PointF aa = new PointF(a.x, a.y);
			PointF bb = new PointF(b.x, b.y);
			PointF origin = mv.getUserPoint();
			PointF originGr = new PointF(0, 0);
			if(!VectorUtils.areEqual(mv.getDestinationPoint(), originGr) &&  
						!VectorUtils.areEqual(mv.getOriginPoint(), originGr) &&
						!VectorUtils.areEqual(mv.getDestinationPoint(), mv.getOriginPoint()))
				{
					int intersections = n.calculateIntersections(aa, bb).size();
					return (intersections == 0 && origin.x != 0 && origin.y != 0);
				}
			return false;
		}
		
		//creates path from starting point to destination
		public List<PointF> createUserPath(PointF a, PointF c)
		{
			List<PointF> path = new ArrayList<PointF>();
			PointF tmp = new PointF(a.x, a.y);
			PointF b = new PointF(c.x, c.y);
			InterceptPoint interS;
			List<InterceptPoint> intersections;
			
			if(a.x != b.x && b.y != a.y)
			{
				path.add(tmp);
				
				while(n.calculateIntersections(tmp, b).size() != 0)
					{
						intersections = n.calculateIntersections(tmp, b);
						
						if(intersections.iterator().hasNext())
						{ 
							if(intersections.iterator().next() != null)
							{
								interS = intersections.iterator().next();
								path.add(alternatePath(interS, b));
								tmp = alternatePath(interS, b);
							}
						}
					
					}
				
				path.add(b);
			}
			return path;
		}
		
		//finds intersections and create alternate path
		private PointF alternatePath(InterceptPoint i, PointF endB) 
		{
			LineSegment wall = i.getLine();
			float[] unitVect = {0,0};
			float[] Point = {0,0};
			PointF pathIdeal = new PointF();
			PointF pathDir;
				
				//finds both ends of walls  and sees which one is closer
				if(VectorUtils.distance(wall.start, endB) > VectorUtils.distance(wall.end, endB)){
					
					pathIdeal = wall.end;
				}
				else 
				{
					pathIdeal = wall.start; 
				}
				
				Point[0] = pathIdeal.x;
				Point[1] = pathIdeal.y;
				float[] Vect = {Point[0] - i.getPoint().x, Point[1] - i.getPoint().y};
				VectorUtils.convertToUnitVector(unitVect, Vect);

			pathDir = new PointF(Point[0] + unitVect[0]*0.005f, Point[1] + unitVect[1]*0.005f);
			
			return pathDir;
		}
		
		//checks if user has finished 
		public boolean finish()
		{
			PointF user = new PointF(mv.getUserPoint().x, mv.getUserPoint().y);
			PointF dest = new PointF(mv.getDestinationPoint().x, mv.getDestinationPoint().y);
			
			//checks if user is within 0.5 of destination
			if(Math.abs(dest.x - user.x) <  0.5 && Math.abs(dest.y - user.y) <  0.5)
			{
				return true;
			}
			return false;
		}
		
		public float getDirection(float absDirection)
		{
			float returnVal = absDirection - directionFactor;
			return returnVal;
		}
		
}

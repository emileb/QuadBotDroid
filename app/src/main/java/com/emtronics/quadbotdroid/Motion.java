package com.emtronics.quadbotdroid;

public class Motion {
	public class servo_status
	{
		public int min = 0;
		public int max = 255;
		public int position = 128 * 256;

		public servo_status()
		{

		}

		public void set_position(int p)
		{
			if (p>255) p=255;
			if (p<0) p=0;
			position = p *256;
		}

		public int get_position()
		{
			return (position / 256);
		}

		public void inc(int ammount)
		{
			if ((position + ammount) < (max * 256))
			{
				position += ammount;
			}
			else
			{
				position = max * 256;
			}
		}

		public void dec(int ammount)
		{
			if ((position - ammount) > (min * 256))
			{
				position -= ammount;
			}
			else
			{
				position = min;
			}
		}
	}


	public enum QB_Mode {MODE_NORMAL,MODE_360};

	public QB_Mode Current_Mode = QB_Mode.MODE_NORMAL;

	public int FR_offset = -16;
	public int BR_offset = -16;
	public int BL_offset = -6;
	public int FL_offset = -10; 

	public int FR_360_pos = 38;
	public int BR_360_pos = 190;
	public int BL_360_pos = 42;
	public int FL_360_pos = 199;

	public int FR = 0;
	public int BR = 0;
	public int BL = 0;
	public int FL = 0; 

	public int L_speed = 0;
	public int R_speed = 0;

	public int L_dir;
	public int R_dir;


	public servo_status[] Servos;

	public  int SERVO_CAM_Y = 1;
	public  int SERVO_CAM_X = 0;
	public  int SERVO_ARM_1 = 7;
	public  int SERVO_ARM_2 = 6;

	public  int SERVO_ARM_TURN = 2;
	public  int SERVO_ARM_GRAB = 3;


	public Motion()
	{
		//
		// TODO: Add constructor logic here
		//
		Servos = new servo_status[8];

		for (int n=0;n<8;n++)
		{
			Servos[n] = new servo_status();
		}
		Servos[SERVO_ARM_1].set_position(250);
		Servos[SERVO_ARM_2].set_position(200);
		
		Current_Mode = QB_Mode.MODE_NORMAL;
		
	}

	public void toggleMode()
	{
		if (Current_Mode == QB_Mode.MODE_360)
			Current_Mode = QB_Mode.MODE_NORMAL;
		else
			Current_Mode = QB_Mode.MODE_360;
	}

	public void processControls(QB_Mode mode, ControlsState controls)
	{
		Current_Mode = mode;
		/*
		if (XInput.Controllers[ 0 ].IsButtonPressed( ControllerButtons.A ))
		{
			Current_Mode = QB_Mode.MODE_NORMAL;
		}

		if (XInput.Controllers[ 0 ].IsButtonPressed( ControllerButtons.B ))
		{
			Current_Mode = QB_Mode.MODE_360;
		}
		*/
		
		
		Mode_Common();


		switch (Current_Mode)
		{
		case MODE_NORMAL:
			Mode_Normal(controls);
			break;
		case MODE_360:
			Mode_360(controls);
			break;
		}
	}

	public void Mode_Common()
	{
/*
		if (XInput.Controllers[ 0 ].IsButtonPressed(ControllerButtons.Up))
		{
			Servos[SERVO_CAM_Y].inc(3*256);
		}


		if (XInput.Controllers[ 0 ].IsButtonPressed(ControllerButtons.Down))
		{
			Servos[SERVO_CAM_Y].dec(3*256);
		}

		if (XInput.Controllers[ 0 ].IsButtonPressed(ControllerButtons.Left))
		{
			Servos[SERVO_CAM_X].inc(3*256);
		}


		if (XInput.Controllers[ 0 ].IsButtonPressed(ControllerButtons.Right))
		{
			Servos[SERVO_CAM_X].dec(3*256);
		}
		*/
	}

	public void Mode_360(ControlsState controls)
	{
		
		int leftX = controls.leftX;
		int leftY =  controls.leftY;
		
		int rightY = -controls.rightY;
		int velocity = controls.velocity;

		
		
		//Wheels
		FR = FR_360_pos;
		BR = BR_360_pos;
		BL = BL_360_pos;
		FL = FL_360_pos;

		R_speed = Math.abs(velocity);
		L_speed = Math.abs(velocity);

		if (velocity >= 0 )
		{
			L_dir = 255;
			R_dir = 0;
		}
		else
		{
			L_dir = 0;
			R_dir = 255;
		}

	}



	public void Mode_Normal(ControlsState controls)
	{
		int leftX = controls.leftX;
		int leftY = controls.leftY;
		
		
		int rightX =  controls.rightX / 2;
		int velocity = controls.velocity;
		
		//Wheels
		int angle = Math.abs(rightX);
		double angle_rad = (3.14159/180) * angle;

		double f_temp = 1/(1/(Math.tan(angle_rad)) + 2);

		double other_angle_rad = Math.atan(f_temp);

		double other_angle = other_angle_rad / (3.14159/180);

		int angle1 = (int)other_angle;

		double r1 = 1/(Math.sin(angle_rad));
		double r2 = 1/(Math.sin(other_angle_rad));

		double ratio;
		if (angle_rad == 0)
			ratio = 1;
		else
			ratio = r2/r1;

		double slow_speed =  Math.abs(velocity) /ratio;

		if (rightX > 0) 
		{
			FR = 128 + angle;
			BR = 128 - angle;
			BL = 128 - angle1;
			FL = 128 + angle1;

			R_speed = (int)slow_speed;
			L_speed = Math.abs(velocity);
		}
		else
		{
			FR = 128 - angle1;
			BR = 128 + angle1;
			BL = 128 + angle;
			FL = 128 - angle;

			R_speed = Math.abs(velocity);
			L_speed = (int)slow_speed;
		}
		
		//Log.d("test","w1=" + BR + ", w2="+FR + ", w3="+FL + ", w4="+BL);
		
		FL += FL_offset;
		FR += FR_offset;

		BL += BL_offset;
		BR += BR_offset;

		/*
		FL += leftX;
		BL += leftX;
		FR += leftX;
		BR += leftX;
*/
		if (FL < 0) FL = 0;
		if (BL < 0) BL = 0;
		if (FR < 0) FR = 0;
		if (BR < 0) BR = 0;

		if (FL > 255) FL = 255;
		if (BL > 255) BL = 255;
		if (FR > 255) FR = 255;
		if (BR > 255) BR = 255;

		if (velocity < 0 )
		{
			L_dir = 0;
			R_dir = 0;
		}
		else
		{
			L_dir = 255;
			R_dir = 255;
		}
	}
}

#include "misc.h"

processor_info cur_processor;

void misc_dump_processor()
{
	// get cpu max freq
	FILE *cpufile = fopen(CPUINFO_MAX, "r");
	if(!cpufile)
		cur_processor.cpumax =0;
	else
	{
		fscanf(cpufile, "%d", &cur_processor.cpumax);
		fclose(cpufile);
	}

	// get cpu min freq
	cpufile = fopen(CPUINFO_MIN, "r");
	if(!cpufile)
		cur_processor.cpumin =0;
	else
	{
		fscanf(cpufile, "%d", &cur_processor.cpumin);
		fclose(cpufile);
	}

	// get scaling cur freq
	cpufile = fopen(CPU_SCALING_CUR, "r");
	if(!cpufile)
		cur_processor.scalcur =0;
	else
	{
		fscanf(cpufile, "%d", &cur_processor.scalcur);
		fclose(cpufile);
	}

	// get scaling max freq
	cpufile = fopen(CPU_SCALING_MAX, "r");
	if(!cpufile)
		cur_processor.scalmax =0;
	else
	{
		fscanf(cpufile, "%d", &cur_processor.scalmax);
		fclose(cpufile);
	}

	// get scaling min freq
	cpufile = fopen(CPU_SCALING_MIN, "r");
	if(!cpufile)
		cur_processor.scalmin =0;
	else
	{
		fscanf(cpufile, "%d", &cur_processor.scalmin);
		fclose(cpufile);
	}

	// get scaling governor
	cpufile = fopen(CPU_SCALING_GOR, "r");
	if(!cpufile)
		strcpy(cur_processor.scalgov, "");
	else
	{
		fscanf(cpufile, "%s", cur_processor.scalgov);
		fclose(cpufile);
	}

	// OMAP3430 temperature
	cpufile = fopen(OMAP_TEMPERATURE, "r");
	if(!cpufile)
		cur_processor.omaptemp = 0;
	else
	{
		fscanf(cpufile, "%d", &cur_processor.omaptemp);
		fclose(cpufile);
	}

/*	// AKM8976A
	int akmfd = open(AKM_DEVICE_NAME, O_RDONLY);
    struct input_event event;

    if(akmfd)
    {
    	__android_log_write(ANDROID_LOG_WARN,"AKM8976A","Enter");

        while (1) {
			  __android_log_write(ANDROID_LOG_WARN,"AKM8976A","Read Event");
              struct input_event event;
              int nread = read(akmfd, &event, sizeof(event));
              if (nread == sizeof(event)) {
                  uint32_t v;
                  if (event.type == EV_ABS)
                  {
        			  __android_log_write(ANDROID_LOG_WARN,"AKM8976A","Read EVABS");

                      //LOGD("type: %d code: %d value: %-5d time: %ds",
                      //        event.type, event.code, event.value,
                      //      (int)event.time.tv_sec);
                      if(event.code == EVENT_TYPE_TEMPERATURE)
                      {
							  __android_log_write(ANDROID_LOG_WARN,"AKM8976A","IS TEMPERATURE");
                              cur_processor.akmtemp = event.value;
                              break;
                      }
                  }
              }
        }
    }
	close(akmfd);
*/


}

int misc_get_processor_cpumax()
{
	return cur_processor.cpumax;
}

int misc_get_processor_cpumin()
{
	return cur_processor.cpumin;
}

int misc_get_processor_scalcur()
{
	return cur_processor.scalcur;
}

int misc_get_processor_scalmax()
{
	return cur_processor.scalmax;
}

int misc_get_processor_scalmin()
{
	return cur_processor.scalmin;
}

void misc_get_processor_scalgov(char* buf)
{
	strcpy(buf, cur_processor.scalgov);
	return;
}

int misc_get_processor_omaptemp()
{
	return cur_processor.omaptemp;
}

power_info cur_powerinfo;

void misc_dump_power()
{
	char buf[128];
	FILE *fp;

	memset(&cur_powerinfo, 0, sizeof(power_info));

	fp = fopen(BATTERY_STATUS_PATH, "r");
    if(fp != 0)
    {
    	fgets(buf, 128, fp);
        strncpy(cur_powerinfo.status, buf, 16);
        fclose(fp);
    }

    fp = fopen(BATTERY_HEALTH_PATH, "r");
    if(fp != 0)
    {
    	fgets(buf, 128, fp);
        sscanf(buf, "%s", cur_powerinfo.health);
        fclose(fp);
    }

    fp = fopen(BATTERY_CAPACITY_PATH, "r");
    if(fp != 0)
    {
    	fgets(buf, 128, fp);
        sscanf(buf, "%d", &cur_powerinfo.capacity);
        fclose(fp);
    }

    fp = fopen(BATTERY_VOLTAGE_PATH, "r");
    if(fp != 0)
    {
    	fgets(buf, 128, fp);
        sscanf(buf, "%d", &cur_powerinfo.voltage);
        fclose(fp);
    }

    fp = fopen(BATTERY_TEMPERATURE_PATH, "r");
    if(fp != 0)
    {
    	fgets(buf, 128, fp);
        sscanf(buf, "%d", &cur_powerinfo.temperature);
        fclose(fp);
    }

    fp = fopen(BATTERY_TECHNOLOGY_PATH, "r");
    if(fp != 0)
    {
    	fgets(buf, 128, fp);
        sscanf(buf, "%s", cur_powerinfo.technology);
        fclose(fp);
    }

    fp = fopen(AC_ONLINE_PATH, "r");
    if(fp != 0)
    {
    	fgets(buf, 128, fp);
        sscanf(buf, "%d", &cur_powerinfo.aconline);
        fclose(fp);
    }

    fp = fopen(USB_ONLINE_PATH, "r");
    if(fp != 0)
    {
    	fgets(buf, 128, fp);
        sscanf(buf, "%d", &cur_powerinfo.usbonline);
        fclose(fp);
    }

}

int misc_get_power_capacity()
{
	return cur_powerinfo.capacity;
}

int misc_get_power_voltage()
{
	return cur_powerinfo.voltage;
}

int misc_get_power_temperature()
{
	return cur_powerinfo.temperature;
}

int misc_get_power_aconline()
{
	return cur_powerinfo.aconline;
}

int misc_get_power_usbonline()
{
	return cur_powerinfo.usbonline;
}

void misc_get_power_health(char *buf)
{
	snprintf(buf, BUFFERSIZE, "%s", cur_powerinfo.health);
	return;
}

void misc_get_power_status(char *buf)
{
	snprintf(buf, BUFFERSIZE, "%s", cur_powerinfo.status);
	return;
}

void misc_get_power_technology(char *buf)
{
	snprintf(buf, BUFFERSIZE, "%s", cur_powerinfo.technology);
	return;
}

/* File System Module */
struct statfs datafs;
struct statfs systemfs;
struct statfs sdcardfs;
struct statfs cachefs;


void misc_dump_filesystem()
{
	memset(&datafs, 0, sizeof(statfs));
	memset(&systemfs, 0, sizeof(statfs));
	memset(&sdcardfs, 0, sizeof(statfs));
	memset(&cachefs, 0, sizeof(statfs));

	statfs(DATA_PATH, &datafs);
	statfs(SYSTEM_PATH, &systemfs);
	statfs(SDCARD_PATH, &sdcardfs);
	statfs(CACHE_PATH, &cachefs);
}

double misc_get_filesystem_systemtotal()
{
	return ((long long)systemfs.f_blocks * (long long)systemfs.f_bsize) / 1024;
}

double misc_get_filesystem_datatotal()
{
	return ((long long)datafs.f_blocks * (long long)datafs.f_bsize) / 1024;
}

double misc_get_filesystem_sdcardtotal()
{
	return ((long long)sdcardfs.f_blocks * (long long)sdcardfs.f_bsize) / 1024;
}

double misc_get_filesystem_cachetotal()
{
	return ((long long)cachefs.f_blocks * (long long)cachefs.f_bsize) / 1024;
}

double misc_get_filesystem_systemused()
{
	return ((long long)(systemfs.f_blocks - (long long)systemfs.f_bfree) * systemfs.f_bsize) / 1024;
}

double misc_get_filesystem_dataused()
{
	return ((long long)(datafs.f_blocks - (long long)datafs.f_bfree) * datafs.f_bsize) / 1024;
}

double misc_get_filesystem_sdcardused()
{
	return ((long long)(sdcardfs.f_blocks - (long long)sdcardfs.f_bfree) * sdcardfs.f_bsize) / 1024;
}

double misc_get_filesystem_cacheused()
{
	return ((long long)(cachefs.f_blocks - (long long)cachefs.f_bfree) * cachefs.f_bsize) / 1024;
}


double misc_get_filesystem_systemavail()
{
	return ((long long)systemfs.f_bfree * (long long)systemfs.f_bsize) / 1024;
}

double misc_get_filesystem_dataavail()
{
	return ((long long)datafs.f_bfree * (long long)datafs.f_bsize) / 1024;
}

double misc_get_filesystem_sdcardavail()
{
	return ((long long)sdcardfs.f_bfree * (long long)sdcardfs.f_bsize) / 1024;
}

double misc_get_filesystem_cacheavail()
{
	return ((long long)cachefs.f_bfree * (long long)cachefs.f_bsize) / 1024;
}

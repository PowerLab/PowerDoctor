#include "osmonitor.h"
#include "log.h"
#include "misc.h"
#include "func.h"

/* check rooted */
#define SU_PATH "/system/bin/su"
static int phone_rooted = 0;

void check_rooted()
{
	struct stat s;
	char *path = getenv("PATH");
	char *checkpath = (char *) malloc(strlen(path)+8);
	char checkfile[256];
	int check, last;

	if(!path || checkpath == 0)
	{
		if(lstat(SU_PATH, &s) < 0)
			phone_rooted = 0;
		else
			phone_rooted = 1;
	}
	else
	{
		phone_rooted = 0;
		last = 0;

		strncpy(checkpath, path, strlen(path));
		for (check = 0; check < strlen(path); check++)
		{
			if(checkpath[check] == ':')
			{
				checkpath[check] = '\x0';
				memset(checkfile, 0, 256);
				snprintf(checkfile, 256, "%s" , &checkpath[last], "/su");
				if(lstat(checkfile, &s) == 0)
				{
					phone_rooted = 1;
					break;
				}
			}
		}
	}

	if(checkpath != 0)
		free(checkpath);

	return;
}

static int initflag = 0;
static pthread_t cpu_threads[1] = {0};

static volatile int thread_action = 1;
static volatile int data_refresh = 1;
static volatile int fast_update = 0;

static volatile int update_process = 0;
static volatile int update_interface = 0;
static volatile int update_network = 0;
static volatile int update_misc = 0;
static volatile int update_dmesg = 0;
static volatile int update_logcat = 0;

static volatile int update_timer = 0;
static volatile int update_span = 2;
static volatile int cpu_usage_bg = 0;

int refresh_data()
{
	if(data_refresh == 1)
	{
		if(update_process == 1)
			ps_refresh();

		if(update_interface == 1)
			if_refresh();

		if(update_network == 1)
		{
			ps_refresh();
			net_refresh();
		}

		if(update_dmesg == 1)
			dmesg_refresh();

		if(update_logcat == 1)
			logcat_refresh();

		if(update_logcat == 1 || update_dmesg == 1)
		{
			if(update_logcat == 1 )//&& logcat_check() == 1)
				data_refresh = 0;

			if(update_dmesg == 1 )// && dmesg_check() == 1)
				data_refresh = 0;
		}
		else
			data_refresh = 0;

		ps_list_work_empty();
		if_list_empty();
		net_list_empty();
	}

	return data_refresh;
}

void *backgoundtask(void *threadid)
{
	while(1)
	{
		while(thread_action == 0)
		{
			if(cpu_usage_bg == 1 && update_timer >= update_span)
			{
				cpu_refresh();
				misc_dump_processor(); //debug
				cpu_refresh_usage();
				mem_dump();
				update_timer = 0;
			}
			else
			{
				sleep(1);
				update_timer++;
			}
		}

		if (fast_update > 0)
		{
			sleep(1);

			if(fast_update == 1)
				fast_update++;
			else
				fast_update = 0;
		}
		else
		{
			if(update_timer < update_span)
			{
				sleep(1);
				update_timer++;
				continue;
			}
		}

		if(update_process == 0 && cpu_usage_bg == 1)
		{
			cpu_refresh();
			cpu_refresh_usage();
			misc_dump_processor(); //debug
			mem_dump();
		}

		if(update_process == 1)
		{
			ps_dump();
			cpu_refresh();
			ps_refresh_load();
			mem_dump();
			misc_dump_processor(); //debug
		}
		else if(update_interface == 1)
		{
			if_dump();
			misc_dump_processor(); //debug
		}
		else if(update_network == 1)
		{
			ps_dump();
			net_dump();
			misc_dump_processor(); //debug
		}
		else if(update_misc == 1)
		{
			misc_dump_processor();
			misc_dump_power();
			misc_dump_filesystem();
		}
		else if(update_dmesg == 1)
		{
			dmesg_dump();
			misc_dump_processor(); //debug
		}
		else if(update_logcat == 1)
		{
			logcat_dump();
			misc_dump_processor(); //debug
		}

		update_timer = 0;
		data_refresh = 1;
	}
}

void library_init()
{
	if(initflag == 1)
		return;

	initflag = 1;

	// check root
	check_rooted();

	// first time initialize
	cpu_init();
	ps_init();
	network_init();
	log_init();

	// get base processor information (for preferences)
	misc_dump_processor();

	pthread_create(&cpu_threads[0], NULL, backgoundtask, (void *)1);

	return;
}

void library_stop()
{
	thread_action = 0;
}

void library_start(int type)
{
	update_process = 0;
	update_interface = 0;
	update_network = 0;
	update_misc = 0;
	update_dmesg = 0;
	update_logcat = 0;

	fast_update = 1;

	switch(type)
	{
	case 1:
		update_process = 1;
		break;
	case 2:
		update_interface = 1;
		break;
	case 3:
		update_network = 1;
		break;
	case 4:
		update_misc = 1;
		break;
	case 5:
		update_dmesg = 1;
		break;
	case 6:
		update_logcat = 1;
		break;
	default:
		update_process = 1;
		break;
	}

	thread_action = 1;
}

/* JNI Interface */
static jint Toggle_Stop(JNIEnv* env, jobject thiz)
{
	library_stop();
	return 1;
}

static jint Toggle_Start(JNIEnv* env, jobject thiz, jint type)
{
	library_start(type);
	return 1;
}

static jint Toggle_Ready(JNIEnv* env, jobject thiz)
{
	return data_refresh;
}

static jint Toggle_Load(JNIEnv* env, jobject thiz)
{
	if(data_refresh == 0)
		return 0;

	refresh_data();

	return 1;
}

static jint Toggle_Refresh(JNIEnv* env, jobject thiz)
{
	fast_update = 1;

	while(fast_update)
		usleep(500);

	return 1;
}

static jint Toggle_Time(JNIEnv* env, jobject thiz, jint time)
{
	if(time < 1)
		update_span = 1;
	else
		update_span = time;

	return 1;
}

static jint Toggle_CPU(JNIEnv* env, jobject thiz, jint value)
{
	cpu_usage_bg = value;
	return cpu_usage_bg;
}

/* ROOTED */
static jint Check_Rooted(JNIEnv* env, jobject thiz)
{
	return phone_rooted;
}


/* CPU */
static jstring CPU_GetUsage(JNIEnv* env, jobject thiz)
{
	char buf[16];
	cpu_get_usage(buf);
	return (*env)->NewStringUTF(env, buf);
}

static jint CPU_GetUsageValue(JNIEnv* env, jobject thiz)
{
	return cpu_get_usagevalue();
}

/* Memory */
static jlong Mem_GetTotal(JNIEnv* env, jobject thiz)
{
	return mem_get_total();
}

static jlong Mem_GetFree(JNIEnv* env, jobject thiz)
{
	return mem_get_free();
}

static jlong Mem_GetCached(JNIEnv* env, jobject thiz)
{
	return mem_get_cached();
}

static jlong Mem_GetBuffer(JNIEnv* env, jobject thiz)
{
	return mem_get_buffers();
}

static jint Processor_GetMax(JNIEnv* env, jobject thiz)
{
	return misc_get_processor_cpumax();
}

static jint Processor_GetMin(JNIEnv* env, jobject thiz)
{
	return misc_get_processor_cpumin();
}

static jint Processor_GetScalMax(JNIEnv* env, jobject thiz)
{
	return misc_get_processor_scalmax();
}

static jint Processor_GetScalMin(JNIEnv* env, jobject thiz)
{
	return misc_get_processor_scalmin();
}

static jint Processor_GetScalCur(JNIEnv* env, jobject thiz)
{
	return misc_get_processor_scalcur();
}

static jstring Processor_GetScalGov( JNIEnv* env, jobject thiz)
{
	char buf[BUFFERSIZE];
	misc_get_processor_scalgov(buf);
	return (*env)->NewStringUTF(env, buf);
}

static jint Processor_GetOMAPTemp(JNIEnv* env, jobject thiz)
{
	return misc_get_processor_omaptemp();
}

static jint Power_GetCapacity(JNIEnv* env, jobject thiz)
{
	return misc_get_power_capacity();
}

static jint Power_GetVoltage(JNIEnv* env, jobject thiz)
{
	return misc_get_power_voltage();
}

static jint Power_GetTemperature(JNIEnv* env, jobject thiz)
{
	return misc_get_power_temperature();
}

static jint Power_GetACOnline(JNIEnv* env, jobject thiz)
{
	return misc_get_power_aconline();
}

static jint Power_GetUSBOnline(JNIEnv* env, jobject thiz)
{
	return misc_get_power_usbonline();
}

static jstring Power_GetHealth( JNIEnv* env, jobject thiz)
{
	char buf[BUFFERSIZE];
	misc_get_power_health(buf);
	return (*env)->NewStringUTF(env, buf);
}

static jstring Power_GetStatus( JNIEnv* env, jobject thiz)
{
	char buf[BUFFERSIZE];
	misc_get_power_status(buf);
	return (*env)->NewStringUTF(env, buf);
}


static jstring Power_GetTechnology( JNIEnv* env, jobject thiz)
{
	char buf[BUFFERSIZE];
	misc_get_power_technology(buf);
	return (*env)->NewStringUTF(env, buf);
}

static jdouble FS_GetSystemMemTotal(JNIEnv* env, jobject thiz)
{
	return misc_get_filesystem_systemtotal();
}

static jdouble FS_GetDataMemTotal(JNIEnv* env, jobject thiz)
{
	return misc_get_filesystem_datatotal();
}

static jdouble FS_GetSDCardMemTotal(JNIEnv* env, jobject thiz)
{
	return misc_get_filesystem_sdcardtotal();
}

static jdouble FS_GetCacheMemTotal(JNIEnv* env, jobject thiz)
{
	return misc_get_filesystem_cachetotal();
}

static jdouble FS_GetSystemMemUsed(JNIEnv* env, jobject thiz)
{
	return misc_get_filesystem_systemused();
}

static jdouble FS_GetDataMemUsed(JNIEnv* env, jobject thiz)
{
	return misc_get_filesystem_dataused();
}

static jdouble FS_GetSDCardMemUsed(JNIEnv* env, jobject thiz)
{
	return misc_get_filesystem_sdcardused();
}

static jdouble FS_GetCacheMemUsed(JNIEnv* env, jobject thiz)
{
	return misc_get_filesystem_cacheused();
}

static jdouble FS_GetSystemMemAvail(JNIEnv* env, jobject thiz)
{
	return misc_get_filesystem_systemavail();
}

static jdouble FS_GetDataMemAvail(JNIEnv* env, jobject thiz)
{
	return misc_get_filesystem_dataavail();
}

static jdouble FS_GetSDCardMemAvail(JNIEnv* env, jobject thiz)
{
	return misc_get_filesystem_sdcardavail();
}

static jdouble FS_GetCacheMemAvail(JNIEnv* env, jobject thiz)
{
	return misc_get_filesystem_cacheavail();
}

/* Process */
static jint PS_SETSORT(JNIEnv* env, jobject thiz, jint sort)
{
	switch (sort)
	{

	case 1:
		ps_set_sort(1); //byPID
		break;

	case 2:
		ps_set_sort(2); //byLoad
		break;

	case 3:
		ps_set_sort(3); //byMem
		break;

	case 4:
		ps_set_sort(4); //byThreads
		break;

	case 5:
		ps_set_sort(5); //byName
		break;

	default:
		ps_set_sort(1);
		break;
	}

	return 1;
}

static jint PS_SETORDER(JNIEnv* env, jobject thiz, jint order)
{
	ps_set_order(order);
	return 1;
}

static jint PS_SETFILTER(JNIEnv* env, jobject thiz, jint filter)
{
	if(filter > 0)
		ps_set_filter(1);
	else
		ps_set_filter(0);

	return 1;
}

static jint PS_SETALGORITHM(JNIEnv* env, jobject thiz, jint algorithm)
{
	if(algorithm > 0)
		ps_set_algorithm(1);
	else
		ps_set_algorithm(0);

	return 1;
}


static jint PS_PID( JNIEnv* env, jobject thiz, jint position)
{
	jint retval = ps_get_pid(position);
	return retval;
}

static jint PS_UID( JNIEnv* env, jobject thiz, jint position)
{
	jint retval =  ps_get_uid(position);
	return retval;
}

static jint PS_Load( JNIEnv* env, jobject thiz, jint position)
{
	jint retval = ps_get_load(position);
	return retval;
}

static jlong PS_UTime( JNIEnv* env, jobject thiz, jint position)
{
	jlong retval = ps_get_utime(position);
	return retval;
}

static jlong PS_STime( JNIEnv* env, jobject thiz, jint position)
{
	jlong retval = ps_get_stime(position);
	return retval;
}


static jlong PS_RSS( JNIEnv* env, jobject thiz, jint position)
{
	jlong retval = ps_get_rss(position);
	return retval;
}

static jint PS_THREAD( JNIEnv* env, jobject thiz, jint position)
{
	jint retval =  ps_get_threadnum(position);
	return retval;
}

static jstring PS_Name( JNIEnv* env, jobject thiz, jint position)
{
	char buf[BUFFERSIZE];
	ps_get_name(position, buf);
	return (*env)->NewStringUTF(env, buf);
}

static jstring PS_Owner( JNIEnv* env, jobject thiz, jint position)
{
	char buf[BUFFERSIZE];
	ps_get_owner(position, buf);
	return (*env)->NewStringUTF(env, buf);
}

static jstring PS_Status( JNIEnv* env, jobject thiz, jint position)
{
	char buf[BUFFERSIZE];
	ps_get_status(position, buf);
	return (*env)->NewStringUTF(env, buf);
}

static jstring PS_NameByUID ( JNIEnv* env, jobject thiz, jint uid)
{
	char buf[BUFFERSIZE];
	ps_get_name_by_uid(uid, buf);
	return (*env)->NewStringUTF(env, buf);
}


static jint PS_Count( JNIEnv* env, jobject thiz)
{
	return ps_list_count();
}

/* Interface */
static jint IF_Next( JNIEnv* env, jobject thiz)
{
	return if_list_nextrecord();
}

static jint IF_Reset(JNIEnv* env, jobject thiz)
{
	if_list_reset();
	return 1;
}

static jstring IF_Name( JNIEnv* env, jobject thiz, jint position)
{
	char buf[BUFFERSIZE];
	if_get_name(position, buf);
	return (*env)->NewStringUTF(env, buf);
}

static jstring IF_Addr( JNIEnv* env, jobject thiz, jint position)
{
	char buf[BUFFERSIZE];
	if_get_addr(position, buf);
	return (*env)->NewStringUTF(env, buf);
}

static jstring IF_Addr6( JNIEnv* env, jobject thiz, jint position)
{
	char buf[BUFFERSIZE];
	if_get_addr6(position, buf);
	return (*env)->NewStringUTF(env, buf);
}


static jstring IF_NetMask( JNIEnv* env, jobject thiz, jint position)
{
	char buf[BUFFERSIZE];
	if_get_netmask(position, buf);
	return (*env)->NewStringUTF(env, buf);
}

static jstring IF_NetMask6( JNIEnv* env, jobject thiz, jint position)
{
	char buf[BUFFERSIZE];
	if_get_netmask6(position, buf);
	return (*env)->NewStringUTF(env, buf);
}

static jstring IF_MAC( JNIEnv* env, jobject thiz, jint position)
{
	char buf[BUFFERSIZE];
	if_get_mac(position, buf);
	return (*env)->NewStringUTF(env, buf);
}

static jstring IF_Scope( JNIEnv* env, jobject thiz, jint position)
{
	char buf[BUFFERSIZE];
	if_get_scope(position, buf);
	return (*env)->NewStringUTF(env, buf);
}

static jstring IF_Flags( JNIEnv* env, jobject thiz, jint position)
{
	char buf[BUFFERSIZE];
	if_get_flags(position, buf);
	return (*env)->NewStringUTF(env, buf);
}

static jint IF_OutSize( JNIEnv* env, jobject thiz, jint position)
{
	jint retval = if_get_outsize(position);
	return retval;
}

static jint IF_InSize( JNIEnv* env, jobject thiz, jint position)
{
	jint retval = if_get_insize(position);
	return retval;
}

static jint IF_Count( JNIEnv* env, jobject thiz)
{
	return if_list_count();
}

/* Connection */
static jint Net_SetIP6To4(JNIEnv* env, jobject thiz, jint value)
{
	net_set_ip6to4(value);
	return 1;
}

static jstring Net_GetProcotol( JNIEnv* env, jobject thiz, jint position)
{
	char buf[BUFFERSIZE];
	net_get_protocol(position, buf);
	return (*env)->NewStringUTF(env, buf);
}

static jstring Net_GetLocalIP( JNIEnv* env, jobject thiz, jint position)
{
	char buf[BUFFERSIZE];
	net_get_localip(position, buf);
	return (*env)->NewStringUTF(env, buf);
}

static jint Net_GetLocalPort( JNIEnv* env, jobject thiz, jint position)
{
	int port = net_get_localport(position);
	return port;
}

static jstring Net_GetRemoteIP( JNIEnv* env, jobject thiz, jint position)
{
	char buf[BUFFERSIZE];
	net_get_remoteip(position, buf);
	return (*env)->NewStringUTF(env, buf);
}

static jint Net_GetRemotePort( JNIEnv* env, jobject thiz, jint position)
{
	int port = net_get_remoteport(position);
	return port;
}

static jstring Net_GetStatus( JNIEnv* env, jobject thiz, jint position)
{
	char buf[BUFFERSIZE];
	net_get_status(position, buf);
	return (*env)->NewStringUTF(env, buf);
}

static jint Net_GetUID( JNIEnv* env, jobject thiz, jint position)
{
	int uid = net_get_uid(position);
	return uid;
}

static jint Net_Count( JNIEnv* env, jobject thiz)
{
	return net_list_count();
}

/* dmesg */
static jint DMesg_Count( JNIEnv* env, jobject thiz)
{
	return dmesg_list_count();
}

static jstring DMesg_GetTime( JNIEnv* env, jobject thiz, jint position)
{
	char buf[BUFFERSIZE];
	dmesg_get_time(position, buf);
	return (*env)->NewStringUTF(env, buf);
}

static jstring DMesg_GetLevel( JNIEnv* env, jobject thiz, jint position)
{
	char buf[BUFFERSIZE];
	dmesg_get_level(position, buf);
	return (*env)->NewStringUTF(env, buf);
}

static jstring DMesg_GetMsg( JNIEnv* env, jobject thiz, jint position)
{
	char buf[BUFFERSIZE*2];
	dmesg_get_msg(position, buf);
	return (*env)->NewStringUTF(env, buf);
}

static jint DMesg_SetFilter( JNIEnv* env, jobject thiz, jint value)
{
	dmesg_set_filter(value);
	return 1;
}

static jint DMesg_SetFilterLevel( JNIEnv* env, jobject thiz, jint value)
{
	/*
	0: KERN_EMERG
	1: KERN_ALERT
	2: KERN_CRIT
	3: KERN_ERR
	4: KERN_WARNING
	5: KERN_NOTICE
	6: KERN_INFO
	7: KERN_DEBUG
	*/

	switch(value)
	{
	case 0:
		dmesg_set_filter_level('0');
		break;
	case 1:
		dmesg_set_filter_level('1');
		break;
	case 2:
		dmesg_set_filter_level('2');
		break;
	case 3:
		dmesg_set_filter_level('3');
		break;
	case 4:
		dmesg_set_filter_level('4');
		break;
	case 5:
		dmesg_set_filter_level('5');
		break;
	case 6:
		dmesg_set_filter_level('6');
		break;
	case 7:
		dmesg_set_filter_level('7');
		break;
	default:
		dmesg_set_filter_level('8');
		break;
	}
	return 1;
}

static jint DMesg_SetFilterString( JNIEnv* env, jobject thiz, jstring value)
{
	jboolean flag = 0;
    const char* str = (char *) (*env)->GetStringUTFChars(env, value, &flag);
    if(str != NULL)
    {
    	dmesg_set_filter_string(str);
    	(*env)->ReleaseStringUTFChars(env, value, str);
    }
	return 1;
}

static jstring DMesg_GetAll( JNIEnv* env, jobject thiz)
{
	char buffer[KLOG_BUF_LEN + 1];
	int n, op;

	op = KLOG_READ_ALL;

	n = klogctl(op, buffer, KLOG_BUF_LEN);
	if (n < 0)
		return (*env)->NewStringUTF(env, "");
	buffer[n] = '\0';

  	return (*env)->NewStringUTF(env, buffer);
}

/* logcat */
static jint Logcat_Count( JNIEnv* env, jobject thiz)
{
	return logcat_list_count();
}

static jint Logcat_SetSource( JNIEnv* env, jobject thiz, jint value)
{
	logcat_set_source(value);
	return 1;
}

static jint Logcat_SetFilter( JNIEnv* env, jobject thiz, jint value)
{
	logcat_set_filter(value);
	return 1;
}

static jint Logcat_SetFilterPID( JNIEnv* env, jobject thiz, jint value)
{
	logcat_set_pid(value);
	return 1;
}

static jint Logcat_SetFilterLevel( JNIEnv* env, jobject thiz, jint value)
{
	logcat_set_filter_level(value);
	return 1;
}

static jint Logcat_SetFilterString( JNIEnv* env, jobject thiz, jstring value)
{
	jboolean flag = 0;
    char* str = (char *) (*env)->GetStringUTFChars(env, value, &flag);
    if(str != NULL)
    {
    	logcat_set_filter_string(str);
    	(*env)->ReleaseStringUTFChars(env, value, str);
    }
	return 1;
}

static jint Logcat_GetLogSize(JNIEnv* env, jobject thiz)
{
	return logcat_get_logsize();
}

static jint Logcat_GetLogCurrentSize(JNIEnv* env, jobject thiz)
{
	return logcat_get_logreadablesize();
}

static jstring Logcat_GetTime( JNIEnv* env, jobject thiz, jint position)
{
	char buf[BUFFERSIZE];
	logcat_get_time(position, buf);
	return (*env)->NewStringUTF(env, buf);
}

static jstring Logcat_GetLevel( JNIEnv* env, jobject thiz, jint position)
{
	char buf[BUFFERSIZE];
	logcat_get_level(position, buf);
	return (*env)->NewStringUTF(env, buf);
}

static jint Logcat_GetPID( JNIEnv* env, jobject thiz, jint position)
{
	int pid = 0;
	pid = logcat_get_pid(position);
	return pid;
}

static jstring Logcat_GetTag( JNIEnv* env, jobject thiz, jint position)
{
	char buf[BUFFERSIZE];
	logcat_get_tag(position, buf);
	return (*env)->NewStringUTF(env, buf);
}

static jstring Logcat_GetMsg( JNIEnv* env, jobject thiz, jint position)
{
	char *buf = (void *) 0;
	buf = logcat_get_msg(position);

	if(buf != (void *) 0)
		return (*env)->NewStringUTF(env, buf);
	else
		return (*env)->NewStringUTF(env, "");
}

/* JNI Load and UnLoad */

static const char *classPathName = "com/eolwral/osmonitor/JNIInterface";

static JNINativeMethod gMethods[] = {
		/* name, signature, funcPtr */
		{ "doTaskStop", "()I", Toggle_Stop},
		{ "doTaskStart", "(I)I", Toggle_Start},
		{ "doDataLoad", "()I", Toggle_Load},
		{ "doDataSwap", "()I", Toggle_Ready},
		{ "doDataRefresh", "()I", Toggle_Refresh},
		{ "doDataTime", "(I)I", Toggle_Time},
		{ "doCPUUpdate", "(I)I", Toggle_CPU},

		/* Rooted */
		{ "GetRooted", "()I", Check_Rooted},

		/* CPU */
		{ "GetCPUUsage", "()Ljava/lang/String;", CPU_GetUsage},
		{ "GetCPUUsageValue", "()I", CPU_GetUsageValue},

		/* Processor */
		{ "GetProcessorMax", "()I", Processor_GetMax },
		{ "GetProcessorMin", "()I", Processor_GetMin },
		{ "GetProcessorScalMax", "()I", Processor_GetScalMax },
		{ "GetProcessorScalMin", "()I", Processor_GetScalMin },
		{ "GetProcessorScalCur", "()I", Processor_GetScalCur },
		{ "GetProcessorOMAPTemp", "()I", Processor_GetOMAPTemp },
		{ "GetProcessorScalGov", "()Ljava/lang/String;", Processor_GetScalGov },

		/* Memory */
		{ "GetMemTotal", "()J", Mem_GetTotal},
		{ "GetMemFree", "()J", Mem_GetFree},
		{ "GetMemCached", "()J", Mem_GetCached},
		{ "GetMemBuffer", "()J", Mem_GetBuffer},

		/* Power */
		{ "GetPowerCapacity", "()I", Power_GetCapacity},
		{ "GetPowerVoltage", "()I", Power_GetVoltage},
		{ "GetPowerTemperature", "()I", Power_GetTemperature},
		{ "GetACOnline", "()I", Power_GetACOnline},
		{ "GetUSBOnline", "()I", Power_GetUSBOnline},
		{ "GetPowerHealth", "()Ljava/lang/String;", Power_GetHealth},
		{ "GetPowerStatus", "()Ljava/lang/String;", Power_GetStatus},
		{ "GetPowerTechnology", "()Ljava/lang/String;", Power_GetTechnology},

		/* Disk */
		{ "GetSystemMemTotal", "()D", FS_GetSystemMemTotal},
		{ "GetDataMemTotal", "()D", FS_GetDataMemTotal},
		{ "GetSDCardMemTotal", "()D", FS_GetSDCardMemTotal},
		{ "GetCacheMemTotal", "()D", FS_GetCacheMemTotal},
		{ "GetSystemMemUsed", "()D", FS_GetSystemMemUsed},
		{ "GetDataMemUsed", "()D", FS_GetDataMemUsed},
		{ "GetSDCardMemUsed", "()D", FS_GetSDCardMemUsed},
		{ "GetCacheMemUsed", "()D", FS_GetCacheMemUsed},
		{ "GetSystemMemAvail", "()D", FS_GetSystemMemAvail},
		{ "GetDataMemAvail", "()D", FS_GetDataMemAvail},
		{ "GetSDCardMemAvail", "()D", FS_GetSDCardMemAvail},
		{ "GetCacheMemAvail", "()D", FS_GetCacheMemAvail},

		/* Process */
		{ "SetProcessFilter", "(I)I", PS_SETFILTER},
		{ "SetProcessAlgorithm", "(I)I", PS_SETALGORITHM},
		{ "SetProcessSort", "(I)I", PS_SETSORT},
		{ "SetProcessOrder", "(I)I", PS_SETORDER},
		{ "GetProcessCounts", "()I", PS_Count},
		{ "GetProcessPID", "(I)I", PS_PID},
		{ "GetProcessUID", "(I)I", PS_UID},
		{ "GetProcessLoad", "(I)I", PS_Load},
		{ "GetProcessSTime", "(I)J", PS_STime},
		{ "GetProcessUTime", "(I)J", PS_UTime},
		{ "GetProcessRSS", "(I)J", PS_RSS},
		{ "GetProcessThreads", "(I)I", PS_THREAD},
		{ "GetProcessName", "(I)Ljava/lang/String;", PS_Name},
		{ "GetProcessOwner", "(I)Ljava/lang/String;", PS_Owner},
		{ "GetProcessStatus", "(I)Ljava/lang/String;", PS_Status},
		{ "GetProcessNamebyUID", "(I)Ljava/lang/String;", PS_NameByUID},

		/* Interface */
		{ "doInterfaceNext", "()I", IF_Next},
		{ "doInterfaceReset", "()I", IF_Reset},
		{ "GetInterfaceCounts", "()I", IF_Count},
		{ "GetInterfaceOutSize", "(I)I", IF_OutSize},
		{ "GetInterfaceInSize", "(I)I", IF_InSize},
		{ "GetInterfaceName", "(I)Ljava/lang/String;", IF_Name},
		{ "GetInterfaceAddr", "(I)Ljava/lang/String;", IF_Addr},
		{ "GetInterfaceAddr6", "(I)Ljava/lang/String;", IF_Addr6},
		{ "GetInterfaceNetMask", "(I)Ljava/lang/String;", IF_NetMask},
		{ "GetInterfaceNetMask6", "(I)Ljava/lang/String;", IF_NetMask6},
		{ "GetInterfaceMac", "(I)Ljava/lang/String;", IF_MAC},
		{ "GetInterfaceScope", "(I)Ljava/lang/String;", IF_Scope},
		{ "GetInterfaceFlags", "(I)Ljava/lang/String;", IF_Flags},

		/* Network */
		{ "SetNetworkIP6To4", "(I)I", Net_SetIP6To4},
		{ "GetNetworkCounts", "()I", Net_Count},
		{ "GetNetworkProtocol", "(I)Ljava/lang/String;", Net_GetProcotol},
		{ "GetNetworkLocalIP", "(I)Ljava/lang/String;", Net_GetLocalIP},
		{ "GetNetworkLocalPort", "(I)I", Net_GetLocalPort},
		{ "GetNetworkRemoteIP", "(I)Ljava/lang/String;", Net_GetRemoteIP},
		{ "GetNetworkRemotePort", "(I)I", Net_GetRemotePort},
		{ "GetNetworkStatus", "(I)Ljava/lang/String;", Net_GetStatus},
		{ "GetNetworkUID", "(I)I", Net_GetUID},

		/* DMesg */
		{ "GetDebugMessageCounts", "()I", DMesg_Count},
		{ "GetDebugMessageLevel", "(I)Ljava/lang/String;", DMesg_GetLevel},
		{ "GetDebugMessageTime", "(I)Ljava/lang/String;", DMesg_GetTime},
		{ "GetDebugMessage", "(I)Ljava/lang/String;", DMesg_GetMsg},
		{ "SetDebugMessageLevel", "(I)I", DMesg_SetFilterLevel},
		{ "SetDebugMessage", "(Ljava/lang/String;)I", DMesg_SetFilterString},
		{ "SetDebugMessageFilter", "(I)I", DMesg_SetFilter},
		{ "GetDebugMessage", "()Ljava/lang/String;", DMesg_GetAll},

		/* Logcat */
		{ "GetLogcatCounts", "()I", Logcat_Count},
		{ "GetLogcatSize", "()I", Logcat_GetLogSize},
		{ "GetLogcatCurrentSize", "()I", Logcat_GetLogCurrentSize},
		{ "GetLogcatLevel", "(I)Ljava/lang/String;", Logcat_GetLevel},
		{ "GetLogcatPID", "(I)I", Logcat_GetPID},
		{ "GetLogcatTime", "(I)Ljava/lang/String;", Logcat_GetTime},
		{ "GetLogcatTag", "(I)Ljava/lang/String;", Logcat_GetTag},
		{ "GetLogcatMessage", "(I)Ljava/lang/String;", Logcat_GetMsg},
		{ "SetLogcatSource", "(I)I", Logcat_SetSource},
		{ "SetLogcatFilter", "(I)I", Logcat_SetFilter},
		{ "SetLogcatPID", "(I)I", Logcat_SetFilterPID},
		{ "SetLogcatLevel", "(I)I", Logcat_SetFilterLevel},
		{ "SetLogcatMessage", "(Ljava/lang/String;)I", Logcat_SetFilterString},
};

jint JNI_OnLoad(JavaVM* vm, void* reserved)
{
	JNIEnv *env;
	jclass cls;

	if ( (*vm)->GetEnv(vm, (void **) &env, JNI_VERSION_1_4) )
		return JNI_ERR;

	cls = (*env)->FindClass(env, classPathName);

	(*env)->RegisterNatives(env, cls, gMethods, sizeof(gMethods)/sizeof(gMethods[0]));

	library_init();

	return JNI_VERSION_1_4;
}

void JNI_OnUnload(JavaVM* vm, void* reserved)
{
	JNIEnv *env;
	jclass cls;

	if ((*vm)->GetEnv(vm, (void **) &env, JNI_VERSION_1_4))
		return;

	cls = (*env)->FindClass(env, classPathName);
	(*env)->UnregisterNatives(env, cls);

	return;
}

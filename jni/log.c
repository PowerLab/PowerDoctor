#include "log.h"
#include "func.h"

void log_init()
{
	dmesg_init();
	logcat_init();
}

/* Dmesg */
long boot_time;
dmesg_item* cur_dmesginfo = (void *) 0;
int cur_dmesg_count = 0;
void *cur_dmesg_list = (void *) 0;

int work_dmesg_count = 0;
void *work_dmesg_list = (void *) 0;
long pre_dmesg_time, cur_dmesg_time;

int filter_dmesg = 0;
char filter_dmesg_level;
char filter_dmesg_string[BUFFERSIZE];

void dmesg_list_add(dmesg_item *new_dmesg)
{
	// create new node
	dmesg_item* new_node = (dmesg_item *) malloc(sizeof(dmesg_item));

	memcpy(new_node, new_dmesg, sizeof(dmesg_item));
	new_node->next = (void *) 0;

	// find node
	if(work_dmesg_list != (void *) 0)
	{
		dmesg_item *end = work_dmesg_list;
		while(end->next != (void *) 0)
			end = end->next;

		end->next = new_node;
	}
	else
	{
		work_dmesg_list = new_node;
	}

	work_dmesg_count++;
}

void dmesg_list_empty()
{
	if(work_dmesg_list == (void *) 0)
		return;

	// reset
	dmesg_item *old_node = (dmesg_item *) work_dmesg_list;
	work_dmesg_list = (void *) 0;
	work_dmesg_count = 0;

	// release memory
	dmesg_item *next_node = (void *) 0;
	while(old_node->next != (void *) 0)
	{
		next_node = (dmesg_item *) old_node->next;
		free(old_node);

		old_node = next_node;
	}
	free(old_node);

	return;
}

int dmesg_list_nextrecord()
{
	if(cur_dmesginfo == (void *) 0)
		cur_dmesginfo = cur_dmesg_list;
	else
		cur_dmesginfo = ((dmesg_item *)cur_dmesginfo)->next;

	if(cur_dmesginfo == (void *) 0)
		return 0;
	return 1;
}

int dmesg_list_count()
{
	return cur_dmesg_count;
}

void dmesg_list_reset()
{
	cur_dmesginfo = (void *) 0;
}

int dmesg_list_setpositon(int position)
{
	if(position == -1)
		return 1;

	dmesg_list_reset();
	while(position >= 0)
	{
		if(!dmesg_list_nextrecord())
			return 0;
		position--;
	}
	return 1;
}

void dmesg_get_level(int position, char *buf)
{
	if(!dmesg_list_setpositon(position))
	{
		buf[0] = 0;
		return;
	}

	switch(cur_dmesginfo->level)
	{
	case '0':
		strncpy(buf, "EMERGENCY", BUFFERSIZE);
		break;
	case '1':
		strncpy(buf, "ALERT", BUFFERSIZE);
		break;
	case '2':
		strncpy(buf, "CRITICAL", BUFFERSIZE);
		break;
	case '3':
		strncpy(buf, "ERROR", BUFFERSIZE);
		break;
	case '4':
		strncpy(buf, "WARNING", BUFFERSIZE);
		break;
	case '5':
		strncpy(buf, "NOTICE", BUFFERSIZE);
		break;
	case '6':
		strncpy(buf, "INFORMATION", BUFFERSIZE);
		break;
	case '7':
		strncpy(buf, "DEBUG", BUFFERSIZE);
		break;
	}

	return;
}

void dmesg_get_time(int position, char *buf)
{
	if(!dmesg_list_setpositon(position) ||
		cur_dmesginfo->sec == 0)
	{
		buf[0] = 0;
		return;
	}

	time_t dmesg_sec = boot_time + cur_dmesginfo->sec;
	struct tm *local = localtime(&dmesg_sec);
	snprintf(buf, BUFFERSIZE, "%02d/%02d/%04d %02d:%02d:%02d", local->tm_mon+1, local->tm_mday,
					local->tm_year+1900, local->tm_hour, local->tm_min, local->tm_sec);
	return;
}

void dmesg_get_msg(int position, char* buf)
{
	if(!dmesg_list_setpositon(position))
	{
		buf[0] = 0;
		return;
	}

	strncpy(buf, cur_dmesginfo->msg, BUFFERSIZE*2);
	return;
}

void dmesg_set_filter(int value)
{
	filter_dmesg = value;
}

void dmesg_set_filter_level(char value)
{
	filter_dmesg_level = value;
}

void dmesg_set_filter_string(const char *value)
{
	strncpy(filter_dmesg_string, value, BUFFERSIZE);
}

void dmesg_dump()
{
	char buffer[KLOG_BUF_LEN + 1];
	char line[BUFFERSIZE*2];
	char *lineptr;
	int n, op, sp, ep;

	dmesg_item cur_dmesg;

	op = KLOG_READ_ALL;

	op = klogctl(op, buffer, KLOG_BUF_LEN);

	if (op < 0)
		return;

	dmesg_list_empty();

	pre_dmesg_time = cur_dmesg_time;

	buffer[op] = 0;

	sp = ep = 0;

	while(((int)(lineptr = strstr(buffer+sp, "\n" )) != 0))
	{
		ep = lineptr - buffer - sp;

		if(ep > BUFFERSIZE)
			ep = BUFFERSIZE;

		strncpy(line, buffer+sp, ep);
		line[ep+1] = '\0';
		sp = sp + ep +1;

		cur_dmesg.sec = 0;
		n = 0;

		if(line[3] == '[')
			n = sscanf(line, "<%c>[%lu.%*06lu] %[^\n]", &cur_dmesg.level, &cur_dmesg.sec, cur_dmesg.msg);
		else
			n = sscanf(line, "<%c>%[^\n]", &cur_dmesg.level, cur_dmesg.msg);
		cur_dmesg.msg[strlen(cur_dmesg.msg)-1] = '\0';

		if(n > 0)
		{
			int filter_item = 1;
			if(filter_dmesg == 1)
			{
				if(filter_dmesg_level != '8')
					if(cur_dmesg.level != filter_dmesg_level)
						filter_item = 0;

				if(strlen(filter_dmesg_string) != 0)
					if(strstr(cur_dmesg.msg, filter_dmesg_string) == 0)
						filter_item = 0;
			}

			if(filter_item == 1)
			{
				cur_dmesg_time = cur_dmesg.sec;
				dmesg_list_add(&cur_dmesg);
			}
		}

		if(sp >= op)
			break;
	}

}

void dmesg_refresh()
{
	do_swapint(&cur_dmesg_count, &work_dmesg_count);
	do_swapptr(&cur_dmesg_list, &work_dmesg_list);
}

int dmesg_check()
{
	if (pre_dmesg_time == cur_dmesg_time)
		return 0;
	return 1;
}

void dmesg_init()
{
	// get uptime
	long uptime = 0;
	FILE *uptime_file = fopen("/proc/uptime", "r");

	if(!uptime_file)
		uptime =0;
	else
	{
		fscanf(uptime_file, "%lu.%*lu", &uptime);
		fclose(uptime_file);
	}
	time_t cur_time = time(0);

	pre_dmesg_time = 0;

	boot_time = cur_time - uptime;

	// init dmesg variables
	strncpy(filter_dmesg_string, "", BUFFERSIZE);
	filter_dmesg_level = '8';
	filter_dmesg = 0;
}

/* LogCat */


// internal
int logcat_size = 0;
int logcat_readable = 0;
int logcat_source = 0;

// filter
int filter_logcat = 0;
int filter_logcat_pid = 0;
android_LogPriority filter_logcat_level;
char filter_logcat_string[BUFFERSIZE];

// cursor
logcat_info *cur_logcatinfo = (void *) 0;
time_t pre_logcat_time, cur_logcat_time;

// logcat list
int work_logcat_count = 0;
void *work_logcat_list = (void *) 0;

int cur_logcat_count = 0;
void *cur_logcat_list = (void *) 0;

void logcat_list_add(logcat_info *new_logcat, char *msg)
{
	// create new node
	logcat_info* new_node = (logcat_info *) malloc(sizeof(logcat_info)+strlen(msg)+2);

	memcpy(new_node, new_logcat, sizeof(logcat_info));
	if(strlen(msg) != 0)
		strcpy(new_node->message, msg);
	else
		strcpy(new_node->message, "");
	new_node->next = (void *) 0;

	// find node
	if(work_logcat_list != (void *) 0)
	{
		logcat_info *end = work_logcat_list;
		while(end->next != (void *) 0)
			end = end->next;

		end->next = new_node;
	}
	else
	{
		work_logcat_list = new_node;
	}

	work_logcat_count++;
}

void logcat_list_empty()
{
	if(work_logcat_list == (void *) 0)
		return;

	// reset
	logcat_info *old_node = (logcat_info *) work_logcat_list;
	work_logcat_list = (void *) 0;
	work_logcat_count = 0;

	// release memory
	logcat_info *next_node = (void *) 0;
	while(old_node->next != (void *) 0)
	{
		next_node = (logcat_info *) old_node->next;
		free(old_node);

		old_node = next_node;
	}
	free(old_node);

	return;
}

int logcat_list_nextrecord()
{
	if(cur_logcatinfo == (void *) 0)
		cur_logcatinfo = cur_logcat_list;
	else
		cur_logcatinfo = ((logcat_info *)cur_logcatinfo)->next;

	if(cur_logcatinfo == (void *) 0)
		return 0;
	return 1;
}

int logcat_list_count()
{
	return cur_logcat_count;
}

void logcat_list_reset()
{
	cur_logcatinfo = (void *) 0;
}

int logcat_list_setpositon(int position)
{
	if(position == -1)
		return 1;

	logcat_list_reset();
	while(position >= 0)
	{
		if(!logcat_list_nextrecord())
			return 0;
		position--;
	}
	return 1;
}

void logcat_set_source(int value)
{
	logcat_source = value;
}

void logcat_set_filter(int value)
{
	filter_logcat = value;
}

void logcat_set_pid(int value)
{
	filter_logcat_pid = value;
}

void logcat_set_filter_level(int value)
{
	filter_logcat_level = (android_LogPriority) value;
}

void logcat_set_filter_string(char *value)
{
	strncpy(filter_logcat_string, value, BUFFERSIZE);
}

int logcat_get_logsize()
{
    return logcat_size;
}

int logcat_get_logreadablesize()
{
    return logcat_readable;
}


void logcat_get_level(int position, char *buf)
{
	if(!logcat_list_setpositon(position))
	{
		buf[0] = 0;
		return;
	}

	switch(cur_logcatinfo->priority)
	{
	case ANDROID_LOG_DEFAULT:
		strncpy(buf, "", BUFFERSIZE);
		break;
	case ANDROID_LOG_VERBOSE:
		strncpy(buf, "VERBOSE", BUFFERSIZE);
		break;
	case ANDROID_LOG_DEBUG:
		strncpy(buf, "DEBUG", BUFFERSIZE);
		break;
	case ANDROID_LOG_INFO:
		strncpy(buf, "INFORMATION", BUFFERSIZE);
		break;
	case ANDROID_LOG_WARN:
		strncpy(buf, "WARNING", BUFFERSIZE);
		break;
	case ANDROID_LOG_ERROR:
		strncpy(buf, "ERROR", BUFFERSIZE);
		break;
	case ANDROID_LOG_FATAL:
		strncpy(buf, "FATAL", BUFFERSIZE);
		break;

	default:
		strncpy(buf, "", BUFFERSIZE);
		break;
	}

	return;
}

void logcat_get_time(int position, char *buf)
{
	if(!logcat_list_setpositon(position))
	{
		buf[0] = 0;
		return;
	}

	struct tm *local = localtime(&cur_logcatinfo->tv_sec);
	snprintf(buf, BUFFERSIZE, "%02d/%02d/%04d %02d:%02d:%02d", local->tm_mon+1, local->tm_mday,
					local->tm_year+1900, local->tm_hour, local->tm_min, local->tm_sec);
	return;
}

int logcat_get_pid(int position)
{
	if(!logcat_list_setpositon(position))
		return 0;

	return cur_logcatinfo->pid;
}

void logcat_get_tag(int position, char* buf)
{
	if(!logcat_list_setpositon(position))
	{
		buf[0] = 0;
		return;
	}

	strncpy(buf, cur_logcatinfo->tag, 64);
	return;
}


char *logcat_get_msg(int position)
{
	if(!logcat_list_setpositon(position))
		return (char *) 0;

	return cur_logcatinfo->message;
}

void logcat_get_readloglines(int logfd)
{
    logcat_list_empty();

    pre_logcat_time = cur_logcat_time;

    while (1) {

        unsigned char buf[LOGGER_ENTRY_MAX_LEN + 1] __attribute__((aligned(4)));
        struct logger_entry *entry = (struct logger_entry *) buf;
        char *tag, *msg;
        int ret;

        logcat_info cur_logcat;

        ret = read(logfd, entry, LOGGER_ENTRY_MAX_LEN);
        if (ret < 0) {
            if (errno == EINTR)
                continue;
            if (errno == EAGAIN)
                break;
        }
        else if (!ret) {
        	break;
        }

        /* NOTE: driver guarantees we read exactly one full entry */

        entry->msg[entry->len] = '\0';
        tag = entry->msg+1;
        msg = entry->msg+(strlen(tag)+2);

        int filter_item = 1;
        if(filter_logcat == 1)
        {
        	if(filter_logcat_pid != 0)
        		if(entry->pid != filter_logcat_pid)
        			filter_item = 0;

        	if(filter_logcat_level != 0)
    			if(entry->msg[0] != filter_logcat_level)
    				filter_item = 0;

        	if(strlen(filter_logcat_string) != 0)
        		if(strstr(msg, filter_logcat_string) == 0 &&
        		   strstr(tag, filter_logcat_string) == 0 )
        			filter_item =0;
        }

        if(filter_item == 1)
        {
            cur_logcat.tv_sec = entry->sec;
   	        cur_logcat.tv_nsec = entry->nsec;
       	    cur_logcat.priority = entry->msg[0];
           	cur_logcat.pid = entry->pid;
           	strcpy(cur_logcat.tag, tag);
           	//strncpy(cur_logcat.message, msg, proc-msg);
           	//cur_logcat.message[proc-msg] = 0;
           	cur_logcat.next = 0;

           	cur_logcat_time = entry->sec;
           	logcat_list_add(&cur_logcat, msg);
        }
    }
}

void logcat_dump()
{
    char *log_device = strdup("/dev/"LOGGER_LOG_MAIN);
    if(logcat_source == 1)
    	log_device = strdup("/dev/"LOGGER_LOG_RADIO);
    else if(logcat_source == 2)
    	log_device = strdup("/dev/"LOGGER_LOG_EVENTS);

    int logfd = open(log_device, O_NONBLOCK);

    if (logfd < 0) return;

	logcat_size = ioctl(logfd, LOGGER_GET_LOG_BUF_SIZE);
    if (logcat_size < 0) logcat_size = 0;

    logcat_readable = ioctl(logfd, LOGGER_GET_LOG_LEN);
    if (logcat_readable < 0) logcat_readable = 0;

    logcat_get_readloglines(logfd);

    close(logfd);

}

void logcat_refresh()
{
	do_swapint(&cur_logcat_count, &work_logcat_count);
	do_swapptr(&cur_logcat_list, &work_logcat_list);
}

int logcat_check()
{
	if(cur_logcat_time != pre_logcat_time)
		return 0;
    return 1;
}

void logcat_init()
{
	strcpy(filter_logcat_string, "");
	filter_logcat_level = ANDROID_LOG_DEFAULT;
	filter_logcat_pid = 0;
	filter_logcat = 0;
	logcat_source = 0;
}

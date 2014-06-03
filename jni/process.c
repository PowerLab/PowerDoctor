#include "process.h"

/* CPU Module */
int cpu_usage = 0;
cpu_info pre_cpu, cur_cpu;
int cpu_usage_bg = 0;

void cpu_dump(void)
{
	FILE *cpu_stat_file = fopen("/proc/stat", "r");
	if(!cpu_stat_file)
		return;

    fscanf(cpu_stat_file, "cpu  %lu %lu %lu %lu %lu %lu %lu ", &cur_cpu.user, &cur_cpu.nice,
			&cur_cpu.system, &cur_cpu.idle, &cur_cpu.iowait, &cur_cpu.irq, &cur_cpu.softirq);

    fclose(cpu_stat_file);
}

void cpu_refresh()
{
    memcpy(&pre_cpu, &cur_cpu, sizeof(cur_cpu));

    cpu_dump();

    return;
}

void cpu_refresh_usage()
{
	unsigned long pre_cpu_time = pre_cpu.user + pre_cpu.nice + pre_cpu.system
					+ pre_cpu.idle + pre_cpu.iowait + pre_cpu.irq + pre_cpu.softirq;
	unsigned long cur_cpu_time = cur_cpu.user + cur_cpu.nice + cur_cpu.system
					 + cur_cpu.idle + cur_cpu.iowait + cur_cpu.irq + cur_cpu.softirq;

	unsigned long total_delta_time = cur_cpu_time - pre_cpu_time;

	unsigned long total_cpu_idle = cur_cpu.idle - pre_cpu.idle;

	cpu_usage = 100 - ((double)(total_cpu_idle *100 / total_delta_time));
}

void cpu_init()
{
	cpu_dump();
	return;
}

void cpu_get_usage(char *buf)
{
	snprintf(buf, 16, "%d%%", cpu_usage);
}

int cpu_get_usagevalue()
{
	return cpu_usage;
}

/* Memory Module */
mem_info cur_meminfo;

void mem_dump()
{
	FILE *mif;
	mem_info imf;

	mif = fopen("/proc/meminfo", "r");
    if(mif !=0)
    {
   		memset(&imf, 0, sizeof(imf));

   		/*MemTotal:      2001372 kB
   		MemFree:         96856 kB
   		Buffers:         20316 kB
   		Cached:        1350032 kB*/

   		fscanf(mif, "MemTotal: %lu kB", &imf.memtotal);
   		if_skipline(mif);
   		fscanf(mif, "MemFree: %lu kB", &imf.memfree);
   		if_skipline(mif);
   		fscanf(mif, "Buffers: %lu kB", &imf.buffers);
   		if_skipline(mif);
   		fscanf(mif, "Cached: %lu kB", &imf.cached);

    	fclose(mif);

    	memcpy(&cur_meminfo, &imf, sizeof(imf));
    }
}

unsigned long mem_get_total()
{
	return cur_meminfo.memtotal;
}

unsigned long mem_get_free()
{
	return cur_meminfo.memfree;
}
unsigned long mem_get_cached()
{
	return cur_meminfo.cached;
}

unsigned long mem_get_buffers()
{
	return cur_meminfo.buffers;
}


/* Process Module*/

int filter_by_system = 0;
int sort_algorithm = 1;

int cur_ps_count;
process_info *cur_ps_list;
process_info* cur_psinfo = (void *) 0;

int work_ps_count;
process_info *work_ps_list;
process_info* work_psinfo = (void *) 0;

int old_ps_count;
process_info *old_ps_list;
process_info* old_psinfo = (void *) 0;

int ps_sort_direction;

void ps_list_work_empty()
{
	work_ps_count = 0;
	ps_list_empty(&work_ps_list);
}

void ps_refresh()
{
	do_swapint(&old_ps_count, &cur_ps_count);
	do_swapptr((void *) &old_ps_list, (void *) &cur_ps_list);

	do_swapint(&work_ps_count, &old_ps_count);
	do_swapptr((void *) &work_ps_list, (void *) &old_ps_list);

	ps_sort();
}

void ps_set_filter(int value)
{
	if(value > 0)
		filter_by_system = 1;
	else
		filter_by_system = 0;
}

void ps_set_algorithm(int value)
{
	if(value > 0)
		sort_algorithm = 1;
	else
		sort_algorithm = 0;
}


void ps_set_order(int value)
{
	if(value == 0)
		ps_sort_direction = 0;
	else
		ps_sort_direction = 1;
}

void ps_set_sort(int value)
{
	switch (value)
	{
	case 1:
		ps_sort_type = byPID;
		break;
	case 2:
		ps_sort_type = byLoad;
		break;
	case 3:
		ps_sort_type = byMem;
		break;
	case 4:
		ps_sort_type = byThreads;
		break;
	case 5:
		ps_sort_type = byName;
		break;
	default:
		ps_sort_type = byPID;
		break;
	}

}

void ps_system_add(process_info *new_ps)
{
	// find system node
	if(work_ps_list != (void *) 0)
	{
		process_info *system = work_ps_list;
		system->delta_load += new_ps->delta_load;
		system->delta_stime += new_ps->delta_stime;
		system->delta_utime += new_ps->delta_utime;
		system->rss += new_ps->rss;
		system->threadnum += new_ps->threadnum;
	}
}
void ps_list_add(process_info *new_ps)
{
	process_info *new_node = (process_info *) malloc(sizeof(process_info));

	memcpy(new_node, new_ps, sizeof(process_info));
	new_node->next = (void *) 0;

	// find node
	if(work_ps_list != (void *) 0)
	{
		process_info *end = work_ps_list;
		while(end->next != (void *) 0)
			end = end->next;
		end->next = new_node;
	}
	else
	{
		work_ps_list = new_node;
	}

	work_ps_count++;
}

void ps_list_empty(process_info **work_ps_list)
{
	if(*work_ps_list == (void *) 0)
		return;

	// reset
	process_info *old_node = (process_info *) *work_ps_list;
	*work_ps_list = (void *) 0;

	// release memory
	process_info *next_node = (void *) 0;
	while(old_node->next != (void *) 0)
	{
		next_node = (process_info *) old_node->next;
		free(old_node);
		old_node = next_node;

	}
	free(old_node);

	return;
}

int ps_list_nextrecord(process_info **work_list, process_info **work_ptr)
{
	if(*work_ptr == (void *) 0)
		*work_ptr = *work_list;
	else
		*work_ptr = (*work_ptr)->next;

	if((*work_ptr) == (void *) 0)
		return 0;
	return 1;
}

void ps_list_reset(process_info **work_ptr)
{
	*work_ptr = (void *) 0;
}


int ps_list_setposition(process_info **work_list, process_info **work_ptr, int position)
{
	if(position == -1)
		return 1;

	ps_list_reset(work_ptr);
	while(position >= 0)
	{
		if(!ps_list_nextrecord(work_list, work_ptr))
			return 0;
		position--;
	}
	return 1;
}


void ps_instance_dump(int pid)
{
	char statline[BUFFERSIZE*2];
    struct stat stats;
    process_info psinfo;
    FILE *ps;
    struct passwd *pw;
    int n;

    memset(&psinfo, 0, sizeof(process_info));

    psinfo.pid = pid;

    snprintf(statline, BUFFERSIZE*2, "/proc/%d", pid);
    stat(statline, &stats);

	psinfo.uid = (int) stats.st_uid;

    pw = getpwuid(stats.st_uid);
    if(pw == 0)
    {
    	snprintf(psinfo.owner, 80, "%d", (int)stats.st_uid);
    }
    else
    {
    	strncpy(psinfo.owner, pw->pw_name, 80);
    }


    snprintf(statline, BUFFERSIZE*2, "/proc/%d/stat", pid);
    ps = fopen(statline, "r");
    if (ps != 0)
    {

        /* Scan rest of string. */
        fscanf(ps, "%*d %*s %c %*d %*d %*d %*d %*d %*d %*d %*d %*d %*d "
                     "%lu %lu %*d %*d %*d %*d %d %*d %*d %*lu %ld",
                     &psinfo.status, &psinfo.delta_utime, &psinfo.delta_stime,
											 &psinfo.threadnum, &psinfo.rss);

        fclose(ps);
    }

    snprintf(statline, BUFFERSIZE*2, "/proc/%d/cmdline", pid);
    ps = fopen(statline, "r");
    if(ps == 0) {
    	n = 0;
    }
    else
    {
    	n = fread(psinfo.name, 1, 80, ps);
    	fclose(ps);
    	if(n < 0) n = 0;
    }
    psinfo.name[n] = 0;

    if(strlen(psinfo.name) == 0)
    {
        snprintf(statline, BUFFERSIZE*2, "/proc/%d/stat", pid);
        ps = fopen(statline, "r");
        if(ps!= 0)
        {
        	n = fscanf(ps, "%d (%s)", &pid, psinfo.name);
        	fclose(ps);
        	if(n == 2)
        		psinfo.name[strlen(psinfo.name)-1] = 0;
        }
    }

    if(filter_by_system == 1)
    {
    	if((strcmp(psinfo.owner, "root") != 0) &&
    	   (strstr(psinfo.name, "/system/") == 0) &&
    	   (strstr(psinfo.name, "/sbin/") == 0))
    		ps_list_add(&psinfo);
    	else
    		ps_system_add(&psinfo);

    }
    else
    {
    	ps_list_add(&psinfo);
    }

}

void ps_dump()
{
	DIR *d;
	struct dirent *de;
	char *namefilter = 0;
	int pidfilter = 0;
	int threads = 0;

	d = opendir("/proc");
	if(d == 0) return;

	if(filter_by_system == 1)
	{
	    process_info psinfo;
	    memset(&psinfo, 0, sizeof(process_info));

	    psinfo.uid = 0;
	    strcpy(psinfo.owner, "root");
	    strcpy(psinfo.name, "System");
	    psinfo.status = 'S';
	    ps_list_add(&psinfo);
	}

	while((de = readdir(d)) != 0)
	{
		if(isdigit(de->d_name[0]))
		{
			int pid = atoi(de->d_name);
			ps_instance_dump(pid);
		}
	}
	closedir(d);
}

void ps_init()
{
	cur_ps_count = 0;
	cur_ps_list = (void *) 0;

	old_ps_count = 0;
	old_ps_list = (void *) 0;

	work_ps_count = 0;
	work_ps_list = (void *) 0;

	ps_sort_direction = 0;
	ps_sort_type = byPID;

	ps_dump();

	return;
}

void ps_uninit()
{
	ps_list_empty(&cur_ps_list);
	ps_list_empty(&old_ps_list);
	ps_list_empty(&work_ps_list);
}

void ps_instance_swap(process_info *x,process_info *y)
{
	process_info temp;
	void *xptr, *yptr;

	xptr = x->next;
	yptr = y->next;

	memcpy(&temp, x, sizeof(process_info));
	memcpy(x, y, sizeof(process_info));
	memcpy(y, &temp, sizeof(process_info));

	x->next = xptr;
	y->next = yptr;
}

void ps_quicksort(process_info **work_list, int left, int right)
{
	int i, j, doswitch;
	process_info *pivot, *current, *swapi, *swapj;


	if (left >= right) { return; }

	ps_list_setposition(work_list, &pivot, left);

    i = left+1;
    j = right;

    while(1) {

    	while(i <= right)
    	{
    		doswitch = 0;

    		ps_list_setposition(work_list, &current, i);

    		switch(ps_sort_type)
    		{

    		case byPID:
    			if(ps_sort_direction == 0)
    			{
    				if (pivot->pid > current->pid)
    					doswitch = 1;
    			}
    			else
    			{
    				if (pivot->pid < current->pid)
    					doswitch = 1;
    			}
    			break;

    		case byLoad:
    			if(ps_sort_direction == 0)
    			{
    				if (pivot->delta_load > current->delta_load)
    					doswitch = 1;
    			}
    			else
    			{
    				if (pivot->delta_load < current->delta_load)
    					doswitch = 1;
    			}
    			break;

    		case byMem:
    			if(ps_sort_direction == 0)
    			{
    				if (pivot->rss > current->rss)
    					doswitch = 1;
    			}
    			else
    			{
    				if (pivot->rss < current->rss)
    					doswitch = 1;
    			}
    			break;

    		case byThreads:
    			if(ps_sort_direction == 0)
    			{
    				if (pivot->threadnum > current->threadnum)
    					doswitch = 1;
    			}
    			else
    			{
    				if (pivot->threadnum < current->threadnum)
    					doswitch = 1;
    			}
    			break;

    		case byName:
    			if(ps_sort_direction == 0)
    			{
    				if(strcmp(pivot->name, current->name) < 0)
    					doswitch = 1;
    			}
    			else
    			{
    				if(strcmp(pivot->name, current->name) > 0)
    					doswitch = 1;
    			}
    		}

			if(doswitch == 1)
				break;

           	i = i +1;
    	}

    	while(j > left)
    	{
    		doswitch = 0;

    		ps_list_setposition(work_list, &current, j);

    		switch(ps_sort_type)
    		{
    		case byPID:
    			if(ps_sort_direction != 0)
    			{
    				if (pivot->pid > current->pid)
    					doswitch = 1;
    			}
    			else
    			{
    				if (pivot->pid < current->pid)
    					doswitch = 1;
    			}
    			break;

    		case byLoad:
    			if(ps_sort_direction != 0)
    			{
    				if (pivot->delta_load > current->delta_load)
    					doswitch = 1;
    			}
    			else
    			{
    				if (pivot->delta_load < current->delta_load)
    					doswitch = 1;
    			}
    			break;

    		case byMem:
    			if(ps_sort_direction != 0)
    			{
    				if (pivot->rss > current->rss)
    					doswitch = 1;
    			}
    			else
    			{
    				if (pivot->rss < current->rss)
    					doswitch = 1;
    			}
    			break;

    		case byThreads:
    			if(ps_sort_direction != 0)
    			{
    				if (pivot->threadnum > current->threadnum)
    					doswitch = 1;
    			}
    			else
    			{
    				if (pivot->threadnum < current->threadnum)
    					doswitch = 1;
    			}
    			break;

    		case byName:
    			if(ps_sort_direction != 0)
    			{
    				if(strcmp(pivot->name, current->name) < 0)
    					doswitch = 1;
    			}
    			else
    			{
    				if(strcmp(pivot->name, current->name) > 0)
    					doswitch = 1;
    			}
    		}

    		if(doswitch == 1)
    			break;

    		j = j -1;
    	}

    	if(i > j)
    		break;

        ps_list_setposition(work_list, &swapi, i);
        ps_list_setposition(work_list, &swapj, j);

        ps_instance_swap(swapi, swapj);
    }

    ps_list_setposition(work_list, &swapj, j);
    ps_list_setposition(work_list, &swapi, left);

    ps_instance_swap(swapi, swapj);

    ps_quicksort(work_list, left, j-1);
    ps_quicksort(work_list, j+1, right);
}

void ps_bubblesort(process_info **work_list, int n)
{
	int i, j, flag, doswitch;
	process_info *current, *next;

	flag = 1;
    for (i = 1 ; i < n && flag; i++)
    {
    	flag = 0;
        for (j = 0; j < n - 1; j++)
        {
        	doswitch = 0;

			if(ps_list_setposition(work_list, &current, j) == 0)
				continue;

			if(ps_list_setposition(work_list, &next, j+1) ==0)
				continue;


        	switch(ps_sort_type)
        	{
        	case byPID:
        		if(ps_sort_direction == 0)
        		{
        			if (next->pid > current->pid)
        				doswitch = 1;
        		}
        		else
        		{
        			if (next->pid < current->pid)
        				doswitch = 1;
        		}
        		break;

        	case byLoad:
        		if(ps_sort_direction == 0)
        		{
        			if (next->delta_load > current->delta_load)
        				doswitch = 1;
        		}
        		else
        		{
        			if (next->delta_load < current->delta_load)
        				doswitch = 1;
        		}
        		break;

        	case byMem:
        		if(ps_sort_direction == 0)
        		{
        			if (next->rss > current->rss)
        				doswitch = 1;
        		}
        		else
        		{
        			if (next->rss < current->rss)
        				doswitch = 1;
        		}
        		break;

        	case byThreads:
        		if(ps_sort_direction == 0)
        		{
        			if (next->threadnum > current->threadnum)
        				doswitch = 1;
        		}
        		else
        		{
        			if (next->threadnum < current->threadnum)
        				doswitch = 1;
        		}
        		break;

        	case byName:
        		if(ps_sort_direction == 0)
        		{
        			if(strcmp(next->name, current->name) < 0)
						doswitch = 1;
        		}
        		else
        		{
        			if(strcmp(next->name, current->name) > 0)
						doswitch = 1;
        		}
        	}

			if(doswitch == 1)
            {
            	ps_instance_swap(next, current);
            	flag = 1;
            }
        }
    }

}

void ps_sort()
{
	if(cur_ps_count <= 0)
		return;

	if( sort_algorithm > 0)
		ps_quicksort(&cur_ps_list, 0, cur_ps_count-1);
	else
		ps_bubblesort(&cur_ps_list, cur_ps_count);
}


void ps_refresh_load()
{
	if(old_ps_count == 0)
    	return;

    unsigned long pre_cpu_time = pre_cpu.user + pre_cpu.nice + pre_cpu.system
					+ pre_cpu.idle + pre_cpu.iowait + pre_cpu.irq + pre_cpu.softirq;
	unsigned long cur_cpu_time = cur_cpu.user + cur_cpu.nice + cur_cpu.system
					 + cur_cpu.idle + cur_cpu.iowait + cur_cpu.irq + cur_cpu.softirq;

	unsigned long total_delta_time = cur_cpu_time - pre_cpu_time;

	cpu_usage = 0;
	unsigned long ps_total_delta_time = 0;
	int work_psptr, old_psptr, ps_load, use_time;

	process_info *work, *old;

	for(work_psptr = 0; work_psptr < work_ps_count; work_psptr++)
	{
		for(old_psptr = 0; old_psptr < old_ps_count; old_psptr++)
		{

			if(ps_list_setposition(&work_ps_list, &work, work_psptr) == 0)
				continue;

			if(ps_list_setposition(&old_ps_list, &old, old_psptr) == 0)
				continue;

			if(old->pid == work->pid)
			{
				unsigned long delta_stime = work->delta_stime - old->delta_stime;
				unsigned long delta_utime = work->delta_utime - old->delta_utime;
				ps_total_delta_time = ps_total_delta_time + delta_stime + delta_utime;
				old_psptr = old_ps_count;
				break;
			}
		}
	}

	if(ps_total_delta_time > total_delta_time)
		total_delta_time = ps_total_delta_time;

	for(work_psptr = 0; work_psptr < work_ps_count; work_psptr++)
	{
		for(old_psptr = 0; old_psptr < old_ps_count; old_psptr++)
		{
			if(ps_list_setposition(&work_ps_list, &work, work_psptr) == 0)
				continue;

			if(ps_list_setposition(&old_ps_list, &old, old_psptr) == 0)
				continue;

			if(old->pid == work->pid)
			{
				unsigned long delta_stime = work->delta_stime - old->delta_stime;
				unsigned long delta_utime = work->delta_utime - old->delta_utime;

				old->delta_load  = (delta_stime + delta_utime) * 100 / total_delta_time;;

				ps_load = (int) old->delta_load;

				if(ps_load > 100 || ps_load < 0)
					ps_load = 0;

				old->load = ps_load;
				cpu_usage += old->load;
				old_psptr = old_ps_count;
				break;
			}
		}
	}
}


int ps_get_pid(int position)
{
	if(cur_ps_count < position || position < 0)
		return 0;

	process_info *current;
	if(ps_list_setposition(&cur_ps_list, &current, position) != 0)
		return current->pid;
	return 0;
}


int ps_get_uid(int position)
{
	if(cur_ps_count < position || position < 0)
		return 0;

	process_info *current;
	if(ps_list_setposition(&cur_ps_list, &current, position) != 0)
		return current->uid;
	return 0;
}

int ps_get_load(int position)
{
	if(cur_ps_count < position || position < 0)
		return 0;

	process_info *current;
	if(ps_list_setposition(&cur_ps_list, &current, position) != 0)
		return current->load;
	return 0;
}

unsigned long ps_get_utime(int position)
{
	if(cur_ps_count < position || position < 0)
		return 0;

	process_info *current;
	if(ps_list_setposition(&cur_ps_list, &current, position) != 0)
		return current->delta_utime;
	return 0;
}

unsigned long ps_get_stime(int position)
{
	if(cur_ps_count < position || position < 0)
		return 0;

	process_info *current;
	if(ps_list_setposition(&cur_ps_list, &current, position) != 0)
		return current->delta_stime;
	return 0;
}


void ps_get_status(int position, char *buf)
{
	if(cur_ps_count < position || position < 0)
	{
		buf[0] = 0;
		return;
	}

	process_info *current;
	if(ps_list_setposition(&cur_ps_list, &current, position) != 0)
		snprintf(buf, BUFFERSIZE, "%c", current->status);
	else
		snprintf(buf, BUFFERSIZE, "%c", 0);

	return;
}

void ps_get_name(int position, char *buf)
{
	if(cur_ps_count < position || position < 0)
	{
		buf[0] = 0;
		return;
	}

	process_info *current;
	if(ps_list_setposition(&cur_ps_list, &current, position) != 0)
		strncpy(buf, current->name, BUFFERSIZE);
	else
		buf[0] = 0;
	return;
}

void ps_get_owner(int position, char *buf)
{
	if(cur_ps_count < position || position < 0)
	{
		buf[0] = 0;
		return;
	}

	process_info *current;
	if(ps_list_setposition(&cur_ps_list, &current, position) != 0)
		strncpy(buf, current->owner, BUFFERSIZE);
	else
		buf[0] = 0;
	return;
}

int ps_get_rss(int position)
{
	if(cur_ps_count < position || position < 0)
		return 0;

	process_info *current;
	if(ps_list_setposition(&cur_ps_list, &current, position) != 0)
		return current->rss*4;
	return 0;
}

int ps_get_threadnum(int position)
{
	if(cur_ps_count < position || position < 0)
		return 0;

	process_info *current;
	if(ps_list_setposition(&cur_ps_list, &current, position) != 0)
		return current->threadnum;
	return 0;
}

void ps_get_name_by_uid(int uid, char *buf)
{
	int search_ps = 0;
	process_info *current;

	buf[0] = 0;
	while( cur_ps_count > search_ps)
	{
		if(!ps_list_setposition(&cur_ps_list, &current, search_ps) != 0)
			break;

		if(current->uid == uid)
		{
			strncpy(buf, current->name, BUFFERSIZE);
			break;
		}

		search_ps++;
	}
	return;
}

long ps_list_count()
{
	return cur_ps_count;
}

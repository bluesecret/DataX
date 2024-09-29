package com.alibaba.datax.core.job.event;

import com.alibaba.datax.common.util.Configuration;
import com.alibaba.datax.core.spi.SPI;

@SPI
public interface JobHook {

    void finished(Configuration configuration, JobStatistics statistics);

}

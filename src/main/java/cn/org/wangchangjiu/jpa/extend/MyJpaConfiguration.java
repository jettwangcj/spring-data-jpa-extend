package cn.org.wangchangjiu.jpa.extend;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * @Classname MyJpaConfiguration
 * @Description
 * @Date 2023/7/31 21:12
 * @Created by wangchangjiu
 */
@Configuration
@EnableJpaRepositories(basePackages = "groot.commodity.dao.repository", repositoryFactoryBeanClass = JpaExtendRepositoryFactoryBean.class)
public class MyJpaConfiguration {
}

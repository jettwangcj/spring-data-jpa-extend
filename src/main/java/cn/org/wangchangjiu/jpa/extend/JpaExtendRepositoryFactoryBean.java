package cn.org.wangchangjiu.jpa.extend;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.support.JpaRepositoryFactoryBean;
import org.springframework.data.jpa.repository.support.SimpleJpaRepository;
import org.springframework.data.repository.core.support.RepositoryFactorySupport;

import javax.persistence.EntityManager;
import java.io.Serializable;

/**
 * @Classname MyJpaRepositoryFactoryBean
 * @Description TODO
 * @Date 2023/7/31 21:14
 * @Created by wangchangjiu
 */
public class JpaExtendRepositoryFactoryBean<R extends JpaRepository<T, I>, T, I extends Serializable>
        extends JpaRepositoryFactoryBean<R, T, I>  {

    public JpaExtendRepositoryFactoryBean(Class<? extends R> repositoryInterface) {
        super(repositoryInterface);
    }

    @Override
    protected RepositoryFactorySupport createRepositoryFactory(EntityManager entityManager) {
        RepositoryFactorySupport factorySupport = new JpaExtendRepositoryFactory(entityManager);
        factorySupport.setRepositoryBaseClass(SimpleJpaRepository.class);
        return factorySupport;
    }

}

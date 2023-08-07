package cn.org.wangchangjiu.jpa.extend;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.transaction.annotation.Transactional;

import java.io.Serializable;
import java.util.List;

/**
 * @Classname JpaExtendRepository
 * @Description
 * @Date 2023/8/2 21:36
 * @Created by wangchangjiu
 */
@NoRepositoryBean
public interface JpaExtendRepository<T, ID extends Serializable> extends JpaRepository<T, ID>, JpaSpecificationExecutor<T> {


     Page<T> findAll(Object paramObj, Pageable pageable);

     @Transactional
     <S extends T> List<S> batchSaveAll(Iterable<S> entities, int batchSize);

}

package cn.org.wangchangjiu.jpa.extend;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.NoRepositoryBean;

import java.io.Serializable;
import java.util.List;

/**
 * @Classname JpaExtendRepository
 * @Description
 * @Date 2023/8/2 21:36
 * @author wangchangjiu
 */
@NoRepositoryBean
public interface JpaExtendRepository<T, ID extends Serializable> extends JpaRepository<T, ID>, JpaObjectParamRepository<T> {


     /**
      *  批量保存所有
      * @param entities
      * @param batchSize
      * @return
      * @param <S>
      */
     <S extends T> List<S> batchSaveAll(Iterable<S> entities, int batchSize);

}

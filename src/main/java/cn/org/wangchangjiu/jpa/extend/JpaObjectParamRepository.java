package cn.org.wangchangjiu.jpa.extend;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.NoRepositoryBean;

import java.util.List;
import java.util.Optional;

/**
 * @author wangchangjiu
 * @Classname JpaObjectParamRepository
 * @Description
 * @Date 2023/12/2 14:18
 */
@NoRepositoryBean
public interface JpaObjectParamRepository<T> extends JpaSpecificationExecutor<T> {

    /**
     *  根据参数分页获取所有
     * @param paramObj
     * @param pageable
     * @return
     */
    Page<T> findAll(Object paramObj, Pageable pageable);


    /**
     *  根据参数 获取一个
     * @param paramObj
     * @return
     */
    Optional<T> findOne(Object paramObj);

    /**
     *  根据参数获取所有
     * @param paramObj
     * @return
     */
    List<T> findAll(Object paramObj);

    /**
     *  根据参数带排序获取所有
     * @param paramObj
     * @param sort
     * @return
     */
    List<T> findAll(Object paramObj, Sort sort);


    /**
     *  根据参数 count
     * @param paramObj
     * @return
     */
    long count(Object paramObj);

}

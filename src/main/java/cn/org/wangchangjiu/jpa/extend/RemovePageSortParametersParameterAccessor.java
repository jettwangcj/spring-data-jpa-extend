package cn.org.wangchangjiu.jpa.extend;

import org.springframework.data.repository.query.Parameters;
import org.springframework.data.repository.query.ParametersParameterAccessor;

import java.util.ArrayList;
import java.util.List;

/**
 * @Classname PageParametersParameterAccessor
 * @Description
 * @Date 2023/8/4 18:40
 * @Created by wangchangjiu
 */
public class RemovePageSortParametersParameterAccessor extends ParametersParameterAccessor {

    private final List<Object> removePageSortValues = new ArrayList<>();

    /**
     * Creates a new {@link ParametersParameterAccessor}.
     *
     * @param parameters must not be {@literal null}.
     * @param values     must not be {@literal null}.
     */
    public RemovePageSortParametersParameterAccessor(Parameters<?, ?> parameters, Object[] values) {
        super(parameters, values);

        for(int i = 0 ; i < values.length ; i ++){
            if(parameters.getPageableIndex() != i && parameters.getSortIndex() != i){
                removePageSortValues.add(values[i]);
            }
        }
    }

    public Object[] getRemovePageSortParameters(){
        return removePageSortValues.toArray();
    }



}

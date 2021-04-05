package com.izhiliu.erp.log;


import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.serializer.SerializerFeature;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

@Getter
@Setter
@Accessors(chain = true)
public class LoggerOp {

    public enum  Status {
        START(1),
        OK(2),
        ERROR(3);
        private  int  integer;

        Status(int integer) {
            this.integer = integer;
        }
    }


    private Object[] argArray;


    private String loginId;

    /**
     *   任务是否成功   start ： 1   ok : 2  error : 3
     */
     private int   status;

    public LoggerOp setStatusPlus(LoggerOp.Status status) {
        this.status = status.integer;
        return  this;
    }

    //    @Getter
//    @Setter
//    @Accessors(chain = true)
//    class Operation{
    /**
     * 操作类别 D：数据库操作 B：业务操作 S：系统操作（如登录注销）O: 其他类别
     */
    private String kind;

    /**
     * 操作类型，C：创建，R：查询，U：更新，D：删除，LI：登录 LO：注销，O：其他操作
     */
    private String type;

    /**
     * 操作代码
     */
    private String code;

    /**
     * 操作名称
     */
    private String name;

    /**
     *
     */
    private String message;

    /**
     * 操作影响的结果数（如查询返回的记录数，或者更新的记录数）
     */
    private Long affected;

    /**
     * 操作返回的结果
     */
    private String result;

    /**
     * 操作的数据，Data对象
     */
//        private  Object data;

//        private  Object[] tags;
//    }

    @Override
    public String toString(){
        return JSONObject.toJSONString(this, SerializerFeature.WriteNullListAsEmpty);
    }


    public LoggerOp error(){
        this.status = Status.ERROR.integer;
        return  this ;
    }
    public LoggerOp start(){
        this.status = Status.START.integer;
        return  this ;
    }
    public LoggerOp ok(){
        this.status = Status.OK.integer;
        return  this ;
    }
    
}

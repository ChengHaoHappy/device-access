package cn.edu.bupt.controller;

import cn.edu.bupt.actor.service.FromServerMsgProcessor;
import cn.edu.bupt.dao.exception.IOTException;
import cn.edu.bupt.dao.page.TextPageData;
import cn.edu.bupt.dao.page.TextPageLink;
import cn.edu.bupt.message.BasicFromServerMsg;
import cn.edu.bupt.pojo.Device;
import cn.edu.bupt.utils.StringUtil;
import com.alibaba.fastjson.JSON;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.async.DeferredResult;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;


@RestController
@RequestMapping("/api/v1/deviceaccess")
public class DeviceController extends BaseController {
    @Autowired
    FromServerMsgProcessor fromServerMsgProcessor;

    public static final String DEVICE_ID = "deviceId";

    //@PreAuthorize("#oauth2.hasScope('all') OR hasAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/tenant/deviceCount", method = RequestMethod.GET)
    public Long getTenantDeviceCount(@RequestParam Integer tenantId)  {
        try {
            return deviceService.findDevicesCount(tenantId);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    //@PreAuthorize("#oauth2.hasScope('all') OR hasAuthority('CUSTOMER_USER')")
    @RequestMapping(value = "/customer/deviceCount", method = RequestMethod.GET)
    public Long getCustomerDeviceCount(@RequestParam Integer tenantId,@RequestParam Integer customerId) {
//        try {
//            if (getCurrentUser().getCustomerId().equals(customerId)||
//                    ((getCurrentUser().getAuthority().equals(Authority.TENANT_ADMIN))&&getCurrentUser().getTenantId().equals(tenantId))) {
        try {
            return deviceService.findCustomerDevicesCount(customerId);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
//            }else{
//                throw new IOTException("You aren't authorized to perform this operation!", IOTErrorCode.AUTHENTICATION);
//            }
//        }catch(Exception e){
//            throw handleException(e);
//        }
    }

    //对设备的操作
    //创建设备
    //@PreAuthorize("#oauth2.hasScope('all') OR hasAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/device", method = RequestMethod.POST)
    public String saveDevice(@RequestBody String device)  {
        try {
            //将提交表单的形式转为json格式提交

            Device device1 = JSON.parseObject(device, Device.class);

            Device savedDevice = checkNotNull(deviceService.saveDevice(device1));

//            deviceService.sendMessage(savedDevice,"新增/更新设备："+savedDevice.getName());
            return savedDevice.toString();
        } catch (Exception e) {
            return onFail(e.toString());
        }
    }

    //改变设备站点ID
    //@PreAuthorize("#oauth2.hasScope('all') OR hasAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/device", method = RequestMethod.PUT)
    public String updateDeviceSiteId(@RequestBody String device)  {
        try {
            //将提交表单的形式转为json格式提交

            Device device1 = JSON.parseObject(device, Device.class);
            Device changedDevice = checkNotNull(deviceService.updateDeviceSiteId(device1.getId(),device1.getSiteId()));
            return changedDevice.toString();
        } catch (Exception e) {
            return onFail(e.toString());
        }
    }

    //删除设备
    //@PreAuthorize("#oauth2.hasScope('all') OR hasAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/device/{deviceId}", method = RequestMethod.DELETE)
    public void deleteDevice(@PathVariable(DEVICE_ID) String strDeviceId) {

        try {
            if (StringUtil.isEmpty(strDeviceId)) {
                throw new Exception("device id can't be empty");
            }
            Device device = deviceService.findDeviceById(toUUID(strDeviceId));
            deviceService.deleteDevice(toUUID(strDeviceId));

//            deviceService.sendMessage(device, "删除设备："+device.getName());
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }
    }


    //通过设备ID查找设备
    //@PreAuthorize("#oauth2.hasScope('all') OR hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/device/{deviceId}", method = RequestMethod.GET)
    public Device getDeviceById(@PathVariable(DEVICE_ID) String strDeviceId) {

        try {
            if (StringUtil.isEmpty(strDeviceId)) {
                throw new Exception("can't be empty");
            }
            Device device = deviceService.findDeviceById(toUUID(strDeviceId));
            return device;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    //通过父设备ID查找设备
    //@PreAuthorize("#oauth2.hasScope('all') OR hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/parentdevices/{parentdeviceId}",params = {"limit"}, method = RequestMethod.GET)
    public List<Device> getDevicesByParentDeviceId(
            @PathVariable("parentdeviceId") String parentDeviceId,
            @RequestParam int limit,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String textSearch,
            @RequestParam(required = false) String idOffset,
            @RequestParam(required = false) String textOffset) throws Exception{
        try{
            TextPageLink pageLink = new TextPageLink(limit, textSearch, idOffset==null?null:toUUID(idOffset), textOffset);
            return checkNotNull(deviceService.findDeviceByParentDeviceId(parentDeviceId, pageLink));
        }catch(Exception e){
            e.printStackTrace();
            return null;
        }
    }

    //@PreAuthorize("#oauth2.hasScope('all') OR hasAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/tenant/devices/SearchCount/{tenantId}", method = RequestMethod.GET)
    public Long getTenantDevicesCountByTextSearch(
            @PathVariable("tenantId") Integer tenantId,
            @RequestParam String textSearch)  {
        try {
            TextPageLink pageLink = new TextPageLink(1,textSearch);
            Long count = deviceService.findDevicesCountWithTextSearch(tenantId,pageLink);
            System.out.println(count);
            return count;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    //@PreAuthorize("#oauth2.hasScope('all') OR hasAuthority('CUSTOMER_USER')")
    @RequestMapping(value = "/customerdevices/SearchCount/{tenantId}/{customerId}", method = RequestMethod.GET)
    public Long getCustomerDevicesCountByTextSearch(
            @PathVariable("tenantId") Integer tenantId,
            @PathVariable("customerId") Integer customerId,
            @RequestParam String textSearch) {
        try {
            TextPageLink pageLink = new TextPageLink(1,textSearch);
            Long count = deviceService.findDevicesCountWithTextSearch(tenantId,customerId,pageLink);
            System.out.println(count);
            return count;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    //@PreAuthorize("#oauth2.hasScope('all') OR hasAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/tenant/devices/{tenantId}", params = {"limit"}, method = RequestMethod.GET)
    public TextPageData<Device> getTenantDevices(
            @PathVariable("tenantId") Integer tenantId,
            @RequestParam int limit,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String textSearch,
            @RequestParam(required = false) String idOffset,
            @RequestParam(required = false) String textOffset) {
        try {
            TextPageLink pageLink = new TextPageLink(limit, textSearch,idOffset==null?null:toUUID(idOffset), textOffset);
            /*TextPageData<Device> ls = deviceService.findDevicesByTenantId(tenantId, pageLink);
            TextPageLink pageLink1 = new TextPageLink(1,textSearch);
            Long count = deviceService.findDevicesCountWithTextSearch(tenantId,pageLink1);
            System.out.println(count);*/
            return checkNotNull(deviceService.findDevicesByTenantId(tenantId, pageLink));
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    //删除tenant下的所有设备
    //@PreAuthorize("#oauth2.hasScope('all')")
    @RequestMapping(value = "/devices/{tenantId}", method = RequestMethod.DELETE)
    public void deleteDevicesByTenantId(@PathVariable("tenantId") Integer tenantId)  {
        try {
            deviceService.deleteDevicesByTenantId(tenantId);
        } catch (Exception e) {
            e.printStackTrace();
            return ;
        }
    }

    //通过tenantID和Name查找设备
    //@PreAuthorize("#oauth2.hasScope('all') OR hasAuthority('TENANT_ADMIN')")
    @RequestMapping(value="/device/{tenantId}/{name}",method = RequestMethod.GET)
    public Optional<Device> getDeviceByTenantIdAndName(@PathVariable("tenantId") Integer tenantId,
                                                       @PathVariable("name") String name) {
        try{
            Optional<Device> optionalDevice = deviceService.findDeviceByTenantIdAndName(tenantId, name);
            return optionalDevice;
        }catch (Exception e){
            e.printStackTrace();
            return null;
        }
    }


    //customer层面的设备操作
    //分配设备给客户
    //@PreAuthorize("#oauth2.hasScope('all') OR hasAuthority('TENANT_ADMIN')")
    @RequestMapping(value="/assign/customer/{deviceId}/{customerId}",method = RequestMethod.GET)
    public Device assignDeviceToCustomer(@PathVariable("deviceId") String deviceId,
                                         @PathVariable("customerId")Integer customerId){

        try{
            Device device = deviceService.assignDeviceToCustomer(toUUID(deviceId), customerId);
            return device;
        }catch (Exception e){
            e.printStackTrace();
            return null;
        }
    }

    //取消分配某个设备给客户
    //@PreAuthorize("#oauth2.hasScope('all') OR hasAuthority('TENANT_ADMIN')")
    @RequestMapping(value="/unassign/customer/{deviceId}",method = RequestMethod.DELETE)
    public Device unassignDeviceFromCustomer(@PathVariable("deviceId")String deviceId) {
        try{
            Device device = deviceService.unassignDeviceFromCustomer(toUUID(deviceId));
            return device;
        }catch(Exception e){
            e.printStackTrace();
            return null;
        }
    }

    //取消分配客户的所有设备
    //@PreAuthorize("#oauth2.hasScope('all') OR hasAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/unassign/{tenantId}/{customerId}",method = RequestMethod.DELETE)
    public void unassignCustomerDevices(@PathVariable("tenantId") Integer tenantId,
                                        @PathVariable("customerId") Integer customerId){
        try{
            deviceService.unassignCustomerDevices(tenantId, customerId);
        }catch (Exception e){
            e.printStackTrace();
            return ;
        }
    }

    //获取客户的所有设备
    //@PreAuthorize("#oauth2.hasScope('all') OR hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/customerdevices/{tenantId}/{customerId}", params = {"limit"}, method = RequestMethod.GET)
    public TextPageData<Device> getDevicesByTenantIdAndCustomerId(
            @PathVariable("tenantId") Integer tenantId,
            @PathVariable("customerId") Integer customerId,
            @RequestParam int limit,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String textSearch,
            @RequestParam(required = false) String idOffset,
            @RequestParam(required = false) String textOffset)  {
        try {
            TextPageLink pageLink = new TextPageLink(limit, textSearch, idOffset==null?null:toUUID(idOffset), textOffset);
            return checkNotNull(deviceService.findDevicesByTenantIdAndCustomerId(tenantId, customerId, pageLink));
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }


    //获取站点下的所有设备
    //@PreAuthorize("#oauth2.hasScope('all') OR hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/sitedevices/{tenantId}/{siteId}", params = {"limit"}, method = RequestMethod.GET)
    public TextPageData<Device> getDevicesByTenantIdAndSiteId(
            @PathVariable("tenantId") Integer tenantId,
            @PathVariable("siteId") Integer siteId,
            @RequestParam int limit,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String textSearch,
            @RequestParam(required = false) String idOffset,
            @RequestParam(required = false) String textOffset)  {
        try {
            TextPageLink pageLink = new TextPageLink(limit, textSearch, idOffset==null?null:toUUID(idOffset), textOffset);
            return checkNotNull(deviceService.findDevicesByTenantIdAndSiteId(tenantId, siteId, pageLink));
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    //分配设备到站点，即更新设备
    //@PreAuthorize("#oauth2.hasScope('all') OR hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
/*    @RequestMapping(value = "/assign/site", method = RequestMethod.POST)
    public String assignDeviceToSite(@RequestBody String device)  {
        try {
            Device sitedevice = JSON.parseObject(device, Device.class);
            return checkNotNull(deviceService.saveDevice(sitedevice)).toString();
        } catch (Exception e) {
            return null;
        }
    }*/

    //@PreAuthorize("#oauth2.hasScope('all') OR hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/{manufactuere}/{deviceType}/{model}/devices/", params = {"limit"}, method = RequestMethod.GET)
    public TextPageData<Device> getDevicesByTenantIdAndCustomerId(
            @PathVariable("manufactuere") String manufactuere,
            @PathVariable("deviceType") String deviceType,
            @PathVariable("model") String model,
            @RequestParam int limit,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String textSearch,
            @RequestParam(required = false) String idOffset,
            @RequestParam(required = false) String textOffset
    ){
        try {
            TextPageLink pageLink = new TextPageLink(limit, textSearch, idOffset==null?null:toUUID(idOffset), textOffset);
            return checkNotNull( deviceService.findDevicesByManufactureAndDeviceTypeAndModel(
                    manufactuere, deviceType,model,pageLink
            ));
        }catch (Exception e){
            e.printStackTrace();
            return null;
        }
    }

    //@PreAuthorize("#oauth2.hasScope('all') OR hasAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/devices/{tenantId}", params = {"limit"}, method = RequestMethod.GET)
    public TextPageData<Device> getDevices(
            @PathVariable("tenantId") Integer tenantId,
            @RequestParam int limit,
            @RequestParam(required = false) String textSearch,
            @RequestParam(required = false) String idOffset,
            @RequestParam(required = false) String textOffset) throws Exception {
        try {
            TextPageLink pageLink = new TextPageLink(limit, textSearch,idOffset==null?null:toUUID(idOffset), textOffset);
            TextPageData<Device> ls = deviceService.findDevices(tenantId,pageLink);
            return ls;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    //@PreAuthorize("#oauth2.hasScope('all') OR hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/device/status/{tenantId}", method = RequestMethod.POST)
    public DeferredResult<ResponseEntity> getDeviceStatus(@RequestBody String devices, @PathVariable Integer tenantId){
        DeferredResult<ResponseEntity> res = new DeferredResult<>();

        try{
            JsonObject jsonObject = (JsonObject)new JsonParser().parse(devices);
            List<String> deviceIds = new ArrayList<>();
            JsonArray Dids = jsonObject.getAsJsonArray("deviceId");
            for(JsonElement element : Dids){
                deviceIds.add(element.getAsString());
            }

            BasicFromServerMsg msg = new BasicFromServerMsg(tenantId.toString(),deviceIds,res);
            fromServerMsgProcessor.process(msg);

            return res;

        }catch (Exception e){
            return null;
        }
    }

    //@PreAuthorize("#oauth2.hasScope('all') OR hasAuthority('SYS_ADMIN')")
    @RequestMapping(value = "/devices/suspend/{tenantId}", method = RequestMethod.PUT)
    public void suspendDevices(@PathVariable("tenantId") Integer tenantId)  {
        try {
            deviceService.suspendedDeviceByTenantId(tenantId);
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }
    }

    //@PreAuthorize("#oauth2.hasScope('all') OR hasAuthority('SYS_ADMIN')")
    @RequestMapping(value = "/devices/activate/{tenantId}", method = RequestMethod.PUT)
    public void activateDevices(@PathVariable("tenantId") Integer tenantId)  {
        try {
            deviceService.activatedDeviceByTenantId(tenantId);
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }
    }

    //分配网关下所有设备包括网关自己
    @RequestMapping(value = "/assignAll/{customerId}", method = RequestMethod.GET)
    public void assignAllDevicesToCustomer(@PathVariable("customerId") Integer customerId, @RequestParam String gateway_user) {
        try {
            if(deviceService.findDeviceByTenantIdAndName(2, gateway_user).isPresent()){
                Device parentDevice = deviceService.findDeviceByTenantIdAndName(2, gateway_user).get();
                UUID pId = parentDevice.getId();
                int gatewayCustomerId = parentDevice.getCustomerId();
                if(gatewayCustomerId == 1){
                    deviceService.assignDeviceToCustomer(pId, customerId);
                    gatewayCustomerId = customerId;
                }
                if(gatewayCustomerId == customerId){
                    List<Device> devices = deviceService.findDeviceByParentDeviceId(pId.toString(), new TextPageLink(255));
                    for(Device de : devices){
                        deviceService.assignDeviceToCustomer(de.getId(),customerId );
                    }
                }else {
                    throw new Exception("The gateway has been assigned!");
                }
            }else{
                throw new Exception("Don't has such gateway!");
            }
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }
    }

    @RequestMapping(value = "/unassign/{customerId}", method = RequestMethod.GET)
    public void unassignGatewayAndDevice(@PathVariable("customerId") Integer customerId, @RequestParam String gateway_name) {
        try {
            if(deviceService.findDeviceByTenantIdAndName(2, gateway_name).isPresent()){
                Device gateway = deviceService.findDeviceByTenantIdAndName(2, gateway_name).get();
                UUID gId = gateway.getId();
                int gatewayCustomerId = gateway.getCustomerId();
                if(gatewayCustomerId == customerId){
                    deviceService.unassignDeviceFromCustomer(gId);
                    List<Device> devices = deviceService.findDeviceByParentDeviceId(gId.toString(), new TextPageLink(255));
                    for(Device de : devices){
                        deviceService.unassignDeviceFromCustomer(de.getId());
                    }
                }else{
                    throw new Exception("Don't has been authorized!");
                }

            }else{
                throw new Exception("Don't has such gateway!");
            }
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }
    }

    /**
     * 解绑网关并删除网管下所有设备
     * @param customerId
     * @param gateway_name
     */
    @RequestMapping(value = "/removeGateway/{customerId}", method = RequestMethod.GET)
    public void removeGateway(@PathVariable("customerId") Integer customerId, @RequestParam String gateway_name) {
        try {
            if(deviceService.findDeviceByTenantIdAndName(2, gateway_name).isPresent()){
                Device gateway = deviceService.findDeviceByTenantIdAndName(2, gateway_name).get();
                UUID gId = gateway.getId();
                int gatewayCustomerId = gateway.getCustomerId();
                // 校验用户操作权限
                if(gatewayCustomerId == customerId){
                    deviceService.unassignDeviceFromCustomer(gId);
                    List<Device> devices = deviceService.findDeviceByParentDeviceId(gId.toString(), new TextPageLink(255));
                    for(Device de : devices){
                        deviceService.deleteDevice(de.getId());
                    }
                }else{
                    throw new Exception("Don't has been authorized!");
                }

            }else{
                throw new Exception("Don't has such gateway!");
            }
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }
    }

    @RequestMapping(value = "/assignGateways", method = RequestMethod.GET)
    public TextPageData<Device> getAllAssignGateways(
            @RequestParam int limit,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String textSearch,
            @RequestParam(required = false) String idOffset,
            @RequestParam(required = false) String textOffset
    ) {
        try{
            TextPageLink pageLink = new TextPageLink(limit, textSearch, idOffset==null?null:toUUID(idOffset), textOffset);
            return checkNotNull(deviceService.findAllAssignGateways("Gantch", "Gateway", pageLink));
        }catch (Exception e){
            e.printStackTrace();
            return null;
        }
    }

}

package com.lsd.eshouse.controller;

import com.google.gson.Gson;
import com.lsd.eshouse.common.vo.R;
import com.lsd.eshouse.common.dto.QiNiuPutResult;
import com.lsd.eshouse.common.vo.ResultVo;
import com.lsd.eshouse.service.HouseService;
import com.lsd.eshouse.service.QiNiuService;
import com.qiniu.common.QiniuException;
import com.qiniu.http.Response;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;

/**
 * 上传图片接口
 *
 * Created by lsd
 * 2020-01-26 17:04
 */
@RestController
public class FileController {

    @Autowired
    private QiNiuService qiNiuService;
    @Autowired
    private HouseService houseService;
    @Autowired
    private Gson gson;

    @PostMapping(value = "admin/upload/photo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public R uploadPhoto(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return R.ok(R.StatusEnum.NOT_VALID_PARAM);
        }
        String fileName = file.getOriginalFilename();
        try {
            InputStream inputStream = file.getInputStream();
            Response response = qiNiuService.uploadFile(inputStream, fileName);
            if (response.isOK()) {
                QiNiuPutResult ret = gson.fromJson(response.bodyString(), QiNiuPutResult.class);
                return R.ok(ret);
            } else {
                return R.ok(response.statusCode, response.getInfo());
            }

        } catch (QiniuException e) {
            Response response = e.response;
            try {
                return R.ok(response.statusCode, response.bodyString());
            } catch (QiniuException e1) {
                e1.printStackTrace();
                return R.ok(R.StatusEnum.INTERNAL_SERVER_ERROR);
            }
        } catch (IOException e) {
            return R.ok(R.StatusEnum.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * 移除图片接口
     */
    @DeleteMapping("admin/house/photo")
    @ResponseBody
    public R removeHousePhoto(@RequestParam(value = "id") Integer houseId) {
        ResultVo result = houseService.removePhoto(houseId);

        if (result.isSuccess()) {
            return R.ok(R.StatusEnum.SUCCESS);
        } else {
            return R.ok(HttpStatus.BAD_REQUEST.value(), result.getMessage());
        }
    }

}

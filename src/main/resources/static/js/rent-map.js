/**
 * Created by 瓦力.
 */
var regionCountMap = {}, // 地区数据
    labels = [], // 标签列表
    params = {
        orderBy: 'lastUpdateTime',
        orderDirection: 'desc'
    },
    customLayer; // 麻点图

function load(city, regions, aggData) {
    // 百度地图API功能
    var map = new BMap.Map("allmap", {minZoom: 12}); // 创建实例。设置地图显示最大级别为城市
    var point = new BMap.Point(city.baiduMapLongitude, city.baiduMapLatitude); // 城市中心
    map.centerAndZoom(point, 12); // 初始化地图，设置中心点坐标及地图级别

    map.addControl(new BMap.NavigationControl({enableGeolocation: true})); // 添加比例尺控件
    map.addControl(new BMap.ScaleControl({anchor: BMAP_ANCHOR_TOP_LEFT})); // 左上角
    map.enableScrollWheelZoom(true); // 开启鼠标滚轮缩放

    // 保存aggData数据到regionCountMap中
    for (var i = 0; i < aggData.length; i++) {
        regionCountMap[aggData[i].key] = aggData[i].count;
    }
    // 绘制地图
    drawRegion(map, regions);

    // 加载左边房源列表的数据
    loadHouseData();

    // 缩放事件监听
    map.addEventListener('zoomend', function (event) {
        mapResize(event.target);
    });
    // 地图拖拽完成事件监听
    map.addEventListener('dragend', function (event) {
        mapResize(event.target);
    });
}

/**
 * 用圆圈标出各个地区的聚合数据
 * @param map
 * @param regionList
 */
function drawRegion(map, regionList) {
    var boundary = new BMap.Boundary();
    var polygonContext = {};
    var regionPoint;
    var textLabel;

    // 遍历处理所有地区
    for (var i = 0; i < regionList.length; i++) {
        // 当前地区经纬度位置点
        regionPoint = new BMap.Point(regionList[i].baiduMapLongitude, regionList[i].baiduMapLatitude);

        // 获取当前地区聚合数据
        var houseCount = 0;
        if (regionList[i].en_name in regionCountMap) {
            houseCount = regionCountMap[regionList[i].en_name];
        }

        // 文本内容
        var textContent = '<p style="margin-top: 20px; pointer-events: none">' +
            regionList[i].cn_name + '</p>' + '<p style="pointer-events: none">' +
            houseCount + '套</p>';
        // 地图中 地区经纬度位置点 上显示的文本
        textLabel = new BMap.Label(textContent, {
            position: regionPoint, // 文本标签位置
            offset: new BMap.Size(-40, 20) // 文本偏移量
        });
        // 文本样式
        textLabel.setStyle({
            height: '78px',
            width: '78px',
            color: '#fff',
            backgroundColor: '#0054a5',
            border: '0px solid rgb(255, 0, 0)',
            borderRadius: "50%",                //圆圈形状
            fontWeight: 'bold',
            display: 'inline',
            lineHeight: 'normal',
            textAlign: 'center',
            opacity: '0.8',
            zIndex: 2,
            overflow: 'hidden'
        });
        // 将文本标签画在地图上，并缓存起来
        map.addOverlay(textLabel);
        labels.push(textLabel);

        // 行政区域多边形覆盖物集合
        polygonContext[textContent] = [];
        // 闭包传参
        (function (textContent) {
            let address = city.cn_name + regionList[i].cn_name;
            // 调用百度地图API获取行政区域边界，第一个参数是详细地点，第二个参数是回调函数
            boundary.get(address, function (rs) {
                var count = rs.boundaries.length; // 行政区域边界点的数量
                if (count === 0) {
                    alert('未能获取当前输入行政区域')
                    return;
                }

                for (var j = 0; j < count; j++) {
                    // 定义多边形覆盖物
                    var polygon = new BMap.Polygon(
                        rs.boundaries[j],   //行政区域的第j个边界点
                        {
                            strokeWeight: 2,       //宽度
                            strokeColor: '#0054a5', //颜色
                            fillOpacity: 0.3,      //填充透明度
                            fillColor: '#0054a5'   //填充颜色
                        }
                    );
                    map.addOverlay(polygon); // 添加多边形覆盖物到地图上
                    polygonContext[textContent].push(polygon); //加入到行政区域多边形覆盖物集合，缓存起来
                    polygon.hide(); // 初始隐藏多边形覆盖物
                }
            })
        })(textContent);

        // 鼠标移动到区域上方的监听器
        textLabel.addEventListener('mouseover', function (event) {
            var label = event.target;
            label.setStyle({backgroundColor: '#1AA591'});
            // 绘出鼠标所在的区域的所有边界点，即显示出多边形覆盖物
            var boundaries = polygonContext[label.getContent()];
            for (var n = 0; n < boundaries.length; n++) {
                boundaries[n].show();
            }
        });
        // 鼠标移出区域上方的监听器
        textLabel.addEventListener('mouseout', function (event) {
            var label = event.target;
            label.setStyle({backgroundColor: '#0054a5'});
            // 隐藏鼠标所在的区域的所有边界点，即隐藏多边形覆盖物
            var boundaries = polygonContext[label.getContent()];
            for (var n = 0; n < boundaries.length; n++) {
                boundaries[n].hide();
            }
        });
        // 鼠标点击区域的监听器
        textLabel.addEventListener('click', function (event) {
            // 获取map对象
            var label = event.target;
            var map = label.getMap();
            // 放大地图
            map.zoomIn();
            // 地图中心点移动到事件的触发点
            map.panTo(event.point);
        });
    }

    if (!customLayer) {
        // 自定义图层
        customLayer = new BMap.CustomLayer({
            geotableId: 208990,
            q: '', // 检索关键字
            tags: '', // 空格分隔的字符串
            filter: '', // 过滤条件，参考：http://lbsyun.baidu.com/index.php?title=lbscloud/api/geosearch
            pointDensityType: BMAP_POINT_DENSITY_HIGH
        });
        map.addTileLayer(customLayer); // 添加自定义图层
        customLayer.addEventListener('onhotspotclick', houseTip); // 单击图层事件
    }

    // 百度POI数据标签
    function houseTip(e) {
        var customPoi = e.customPoi; // poi的默认字段
        var contentPoi = e.content; // poi的自定义字段

        var content = '<p style="width:280px;margin: 0; line-height: 20px;">地址：' +
            customPoi.address + '<br/>价格：' + contentPoi.price + '元/月<br/>面积：'
            + contentPoi.area + '平方米</p>';
        var opts = {
            width: 200, // 信息窗口宽度
            height: 100, // 信息窗口高度
            title: contentPoi.title, // 信息窗口标题
            enableMessage: true, // 设置允许信息窗发送信息
            message: contentPoi.address
        };

        // 创建信息窗口对象
        var infoWindow = new BMap.InfoWindow("位置：" + contentPoi.address, opts);
        var point = new BMap.Point(customPoi.point.lng, customPoi.point.lat);

        // 搜索信息提示框
        var searchInfoWindow = new BMapLib.SearchInfoWindow(map, content, {
            title: customPoi.title, // 标题
            width: 290,
            height: 60,
            panel: "panel", // 搜索结果面板
            enableAutoPan: false, // 关闭自动平移 防止触发mapResize
            enableSendToPhone: true, // 是否显示发送到手机按钮
            searchTypes: [
                BMAPLIB_TAB_SEARCH, // 周边检索
                // BMAPLIB_TAB_TO_HERE, // 到这里去
                // BMAPLIB_TAB_FROM_HERE // 从这里出发
            ]
        });

        var marker = new BMap.Marker(point); // 创建marker标注
        marker.addEventListener("click", function (e) { // 点击去除
            map.removeOverlay(e.target);
        });

        marker.setAnimation(BMAP_ANIMATION_BOUNCE); // 跳动的动画
        searchInfoWindow.open(marker);
        map.addOverlay(marker);
    }
}

/**
 * 根据地图缩放级别查询地图当前视野边界范围内的房源
 * @param _map event.target
 */
function mapResize(_map) {
    var bounds = _map.getBounds(),
        southWest = bounds.getSouthWest(), // 西南角
        northEast = bounds.getNorthEast(); // 东北角
    var zoomLevel = _map.getZoom();        // 缩放级别
    // 把获取到的当前视野边界参数放入请求参数中
    params = {
        level: zoomLevel,
        leftLongitude: southWest.lng,  // 左上角
        leftLatitude: northEast.lat,
        rightLongitude: northEast.lng, // 右下角
        rightLatitude: southWest.lat
    };
    // 缩放级别<13的时候显示所有地区的聚合信息标签
    if (zoomLevel < 13) {
        for (var i = 0; i < labels.length; i++) {
            labels[i].show();
        }
    } else {    // 放大的时候重新加载视野内房源，隐藏所有地区的聚合信息标签
        loadHouseData();
        for (var i = 0; i < labels.length; i++) {
            labels[i].hide();
        }
    }
}

/**
 * 请求房源数据 并且进行渲染
 */
function loadHouseData() {
    var target = '&'; // 拼接参数
    $.each(params, function (key, value) {
        target += (key + '=' + value + '&');
    });

    $('#house-flow').html('');
    layui.use('flow', function () {
        var $ = layui.jquery; //不用额外加载jQuery，flow模块本身是有依赖jQuery的，直接用即可。
        var flow = layui.flow;
        flow.load({
            elem: '#house-flow', //指定列表容器
            scrollElem: '#house-flow',
            done: function (page, next) { //到达临界点（默认滚动触发），触发下一页
                //以jQuery的Ajax请求为例，请求下一页数据（注意：page是从2开始返回）
                var lis = [],
                    start = (page - 1) * 3;

                var cityName = $('#cityEnName').val();
                $.get('/rent/house/map/houses?cityEnName=' + cityName + '&start=' + start + '&size=3' + target,
                    function (res) {
                        if (res.code !== 200) {
                            lis.push('<li>数据加载错误</li>');
                        } else {
                            layui.each(res.data, function (index, house) {
                                var direction;
                                switch (house.direction) {
                                    case 1:
                                        direction = '朝东';
                                        break;
                                    case 2:
                                        direction = '朝南';
                                        break;
                                    case 3:
                                        direction = '朝西';
                                        break;
                                    case 4:
                                        direction = '朝北';
                                        break;
                                    case 5:
                                    default:
                                        direction = '南北';
                                        break;
                                }
                                ;

                                var tags = '';
                                for (var i = 0; i < house.tags.length; i++) {
                                    tags += '<span class="item-tag-color_2 item-extra">' + house.tags[i] + '</span>';
                                }
                                var li = '<li class="list-item"><a href="/rent/house/show/' + house.id + '" target="_blank"'
                                    + ' title="' + house.title + '"data-community="1111027382235"> <div class="item-aside">'
                                    + '<img src="' + house.cover + '?imageView2/1/w/116/h/116"><div class="item-btm">'
                                    + '<span class="item-img-icon"><i class="i-icon-arrow"></i><i class="i-icon-dot"></i>'
                                    + '</span>&nbsp;&nbsp;</div></div><div class="item-main"><p class="item-tle">'
                                    + house.title + '</p><p class="item-des"> <span>' + house.room + '室' + house.parlour + '厅'
                                    + '</span><span>' + house.area + '平米</span> <span>' + direction + '</span>'
                                    + '<span class="item-side">' + house.price + '<span>元/月</span></span></p>'
                                    + '<p class="item-community"><span class="item-replace-com">' + house.district + '</span>'
                                    + '<span class="item-exact-com">' + house.district + '</span></p><p class="item-tag-wrap">'
                                    + tags + '</p></div></a></li>';

                                lis.push(li);
                            });
                        }

                        //执行下一页渲染，第二参数为：满足“加载更多”的条件，即后面仍有分页
                        //pages为Ajax返回的总页数，只有当前页小于总页数的情况下，才会继续出现加载更多
                        next(lis.join(''), res.more);
                    });
            }
        });
    });
}

// 排序切换
$('ol.order-select li').on('click', function () {
    $('ol.order-select li.on').removeClass('on');
    $(this).addClass('on');
    params.orderBy = $(this).attr('data-bind');
    params.orderDirection = $(this).attr('data-direction');
    loadHouseData();
});

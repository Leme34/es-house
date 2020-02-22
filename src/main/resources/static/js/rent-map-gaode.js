/**
 * Created by 瓦力.
 */
var regionCountMap = {}, // 地区数据
    labels = [], // 标签列表
    params = {
        orderBy: 'lastUpdateTime',
        orderDirection: 'desc'
    },
    cloudLayer; // 麻点图

function load(city, regions, aggData) {
    // 创建地图
    var map = new AMap.Map('allmap', {
        center: [city.baiduMapLongitude, city.baiduMapLatitude],
        zoom: 13,   //缩放级别
        resizeEnable: false //是否监控地图容器尺寸变化
    });
    // 引入工具条插件
    AMap.plugin([
        'AMap.ToolBar',
        'AMap.Scale',
        'AMap.OverView',
        'AMap.MapType',
        'AMap.Geolocation'], function () {
        var toolbar = new AMap.ToolBar();
        // 在图面添加工具条控件，工具条控件集成了缩放、平移、定位等功能按钮在内的组合控件
        map.addControl(new AMap.ToolBar());
        // 在图面添加比例尺控件，展示地图在当前层级和纬度下的比例尺
        map.addControl(new AMap.Scale());
        // 在图面添加鹰眼控件，在地图右下角显示地图的缩略图
        map.addControl(new AMap.OverView({isOpen: true}));
        // 在图面添加类别切换控件，实现默认图层与卫星图、实施交通图层之间切换的控制
        map.addControl(new AMap.MapType());
        // 在图面添加定位控件，用来获取和展示用户主机所在的经纬度位置
        map.addControl(new AMap.Geolocation());
    });
    // 缩放事件监听，文档说明：https://lbs.amap.com/api/javascript-api/reference/event#mapsevent
    map.on('zoomend', function (event) {
        // this为调用on方法的对象本身
        mapResize(event, this);
    });
    // 地图拖拽完成事件监听
    map.on('mouseup', function (event) {
        mapResize(event, this);
    });
    // 保存aggData数据到regionCountMap中
    for (let i = 0; i < aggData.length; i++) {
        regionCountMap[aggData[i].key] = aggData[i].count;
    }

    // 绘制地区
    drawRegion(map, regions);

    // 加载左边房源列表的数据
    loadHouseData();

    // 加载云麻点数据层
    addCloudLayer(map);

}

/**
 * 用圆圈标出各个地区的聚合数据，并把各个行政地区描边
 * @param map
 * @param regionList 行政地区列表
 */
function drawRegion(map, regionList) {
    var polygonContext = {};

    // 遍历处理所有地区
    for (var i = 0; i < regionList.length; i++) {
        // 获取当前地区聚合数据
        var houseCount = 0;
        if (regionList[i].en_name in regionCountMap) {
            houseCount = regionCountMap[regionList[i].en_name];
        }
        // 文本内容
        var textContent = '<p style="margin-top: 20px; pointer-events: none">' +
            regionList[i].cn_name + '</p>' + '<p style="pointer-events: none">' +
            houseCount + '套</p>';
        // 创建纯文本标记
        var textMarker = new AMap.Text({
            text: textContent,
            anchor: 'center', // 设置文本标记锚点
            offset: new AMap.Pixel(-20, 20), // 基于anchor的偏移量
            cursor: 'pointer',
            style: {
                'height': '78px',
                'width': '78px',
                'font-weight': 'bold',
                // 'display': 'inline',
                'opacity': '0.8',
                'z-index': 2,
                'overflow': 'hidden',
                'border-radius': '50rem',
                backgroundColor: '#0054a5',
                'text-align': 'center',
                'line-height': 'normal',
                'font-size': '20px',
                'color': '#fff'
            },
            position: [regionList[i].baiduMapLongitude, regionList[i].baiduMapLatitude]
        });
        // 将文本标签画在地图上，并缓存起来
        textMarker.setMap(map);
        labels.push(textMarker);

        // 行政区域多边形覆盖物集合
        polygonContext[textContent] = [];
        // 闭包传参
        (function (textContent) {
            // 获取行政区划边界的插件
            AMap.plugin('AMap.DistrictSearch', function () {
                // 创建行政区查询对象
                var district = new AMap.DistrictSearch({
                    // 返回行政区边界坐标等具体信息
                    extensions: 'all',
                    // 设置查询行政区级别为 区
                    level: 'district'
                });
                let regionName = regionList[i].cn_name;
                district.search(regionName, function (status, result) {
                    // 获取朝阳区的边界信息
                    var bounds = result.districtList[0].boundaries;
                    if (bounds) {
                        for (var i = 0, l = bounds.length; i < l; i++) {
                            //生成行政区划polygon
                            var polygon = new AMap.Polygon({
                                map: map,
                                strokeWeight: 2,       //宽度
                                strokeColor: '#0054a5', //颜色
                                path: bounds[i],
                                fillOpacity: 0.3,      //填充透明度
                                fillColor: '#0054a5'   //填充颜色
                            });
                            polygonContext[textContent].push(polygon); //加入到行政区域多边形覆盖物集合，缓存起来
                            polygon.hide(); // 初始隐藏多边形覆盖物
                        }
                    }
                })
            });
        })(textContent);
        // 鼠标移动到区域上方的监听器
        textMarker.on('mouseover', function (event) {
            let text = event.target;
            text.setStyle({backgroundColor: '#1AA591'});
            // 绘出鼠标所在的区域的所有边界点，即显示出多边形覆盖物
            let boundaries = polygonContext[text.getText()];
            for (var n = 0; n < boundaries.length; n++) {
                boundaries[n].show();
            }
        });
        // 鼠标移出区域上方的监听器
        textMarker.on('mouseout', function (event) {
            let text = event.target;
            text.setStyle({backgroundColor: '#0054a5'});
            // 隐藏鼠标所在的区域的所有边界点，即隐藏多边形覆盖物
            let boundaries = polygonContext[text.getText()];
            for (var n = 0; n < boundaries.length; n++) {
                boundaries[n].hide();
            }
        });
        // 鼠标点击区域的监听器
        textMarker.on('click', function (event) {
            // 获取map对象
            let text = event.target;
            let map = text.getMap();
            // 放大地图
            map.zoomIn();
            // 地图中心点移动到事件的触发点
            map.panTo(text.getPosition());
        });
    }
}

/**
 * 根据地图缩放级别查询地图当前视野边界范围内的房源
 * @param event 触发事件对象(event)
 * @param _map 地图实例
 */
function mapResize(event, _map) {
    // 获取视野边界
    var bounds = _map.getBounds(),
        southWest = bounds.southwest, // 西南角
        northEast = bounds.northeast; // 东北角
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
    if (zoomLevel <= 13) {
        for (let i = 0; i < labels.length; i++) {
            labels[i].show();
        }
    } else {    // 放大的时候重新加载视野内房源，隐藏所有地区的聚合信息标签
        loadHouseData();
        for (let i = 0; i < labels.length; i++) {
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


/**
 * 使用CloudDataLayer在地图上展示云麻点数据
 * 文档：https://lbs.amap.com/api/javascript-api/reference/cloudlayer
 */
function addCloudLayer(map) {
    if (!cloudLayer) {
        //加载云图层插件
        map.plugin('AMap.CloudDataLayer', function () {
            var layerOptions = {
                map: map,                   //要叠加该图层的Map对象
                // query: {keywords: '公园'},   //要显示云数据的筛选条件
                clickable: true
            };
            var cloudDataLayer = new AMap.CloudDataLayer('5e4b4d0595c71d0ea5da4665', layerOptions); //实例化云图层类
            cloudDataLayer.setMap(map);
            // 点击监听
            cloudDataLayer.on('click', function (result) {
                //回调函数的参数result.data是点击元素的一行记录的信息（类型为CloudData）
                var clouddata = result.data;
                var photo = [];
                if (clouddata._image[0]) {  //如果有上传的图片
                    photo = ['<img width=240 height=100 src="' + clouddata._image[0]._preurl + '"><br>'];
                }
                // 信息窗口
                var infoWindow = new AMap.InfoWindow({
                    content: "<span class='title'>" + clouddata._name +
                        "</span><hr/>" + photo.join("") +
                        "地址：" + clouddata._address
                        + "<br />" +
                        "面积：" + clouddata.area
                        + "<br />" +
                        "价格：" + clouddata.price,
                    size: new AMap.Size(0, 0),
                    autoMove: true,
                    offset: new AMap.Pixel(0, -25)
                });
                infoWindow.open(map, clouddata._location);
            });
        });
    }
}

// 排序切换
$('ol.order-select li').on('click', function () {
    $('ol.order-select li.on').removeClass('on');
    $(this).addClass('on');
    params.orderBy = $(this).attr('data-bind');
    params.orderDirection = $(this).attr('data-direction');
    loadHouseData();
});

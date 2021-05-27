import os
# comment out below line to enable tensorflow logging outputs
from collections import defaultdict
os.environ['TF_CPP_MIN_LOG_LEVEL'] = '3'
import time
import os
import datetime
import tensorflow as tf
physical_devices = tf.config.experimental.list_physical_devices('GPU')
if len(physical_devices) > 0:
    tf.config.experimental.set_memory_growth(physical_devices[0], True)
from absl import app, flags, logging
from absl.flags import FLAGS
import utils as utils
from yolov4 import filter_boxes
from tensorflow.python.saved_model import tag_constants
from config import cfg
from PIL import Image
import cv2
import numpy as np
import matplotlib.pyplot as plt
from tensorflow.compat.v1 import ConfigProto
from tensorflow.compat.v1 import InteractiveSession
# deep sort imports
from deep_sort import preprocessing, nn_matching
from deep_sort.detection import Detection
from deep_sort.tracker import Tracker
from tools import generate_detections as gdet

class Detector(object):

    frame_num = 0
    scooter_list = []
    rider_list = []
    riders = defaultdict(list)
    riding_state = defaultdict(list)
    scooter_time = defaultdict(list)
    not_ridecount = defaultdict(list)
    rider_time = defaultdict(list)
    old_riders = []
    save_cnt = defaultdict(list)

    def init(self, Flags):
        # Definition of the parameters
        self.max_cosine_distance = 0.4
        self.nn_budget = None
        self.nms_max_overlap = 1.0
        # initialize deep sort
        self.model_filename = 'model_data/mars-small128.pb'
        self.encoder = gdet.create_box_encoder(self.model_filename, batch_size=1)
        # calculate cosine distance metric
        self.metric = nn_matching.NearestNeighborDistanceMetric("cosine", self.max_cosine_distance, self.nn_budget)
        # initialize tracker
        self.tracker = Tracker(self.metric)

        # Object Detector 설정을 로드
        self.config = ConfigProto()
        self.config.gpu_options.allow_growth = True
        self.session = InteractiveSession(config=self.config)
        self.STRIDES, ANCHORS, NUM_CLASS, XYSCALE = utils.load_config(FLAGS)
        self.input_size = FLAGS.size
        self.video_path = FLAGS.video

        # YOLO 모델 로드
        self.saved_model_loaded = tf.saved_model.load(FLAGS.weights, tags=[tag_constants.SERVING])
        self.infer = self.saved_model_loaded.signatures['serving_default']

        # begin video capture
        try:
            self.vid = cv2.VideoCapture(int(self.video_path))
        except:
            self.vid = cv2.VideoCapture(self.video_path)

        self.out = None

        # get video ready to save locally if flag is set
        if FLAGS.output:
            # by default VideoCapture returns float instead of int
            self.width = int(self.vid.get(cv2.CAP_PROP_FRAME_WIDTH))
            self.height = int(self.vid.get(cv2.CAP_PROP_FRAME_HEIGHT))
            self.fps = int(self.vid.get(cv2.CAP_PROP_FPS))
            self.codec = cv2.VideoWriter_fourcc(*FLAGS.output_format)
            self.out = cv2.VideoWriter(FLAGS.output, self.codec, self.fps, (self.width, self.height))

    # 인식된 사람과 전동스쿠터들 중에서, 좌표상으로 봤을 때 탑승중으로 판단되는 지와 사람이 탑승 중인스쿠터 객체를 리턴
    def isRide(self, scooter_list, person):

        # 현재 박스 포맷을 리턴함(min x, miny, max x, max y) numpy array값으로
        pbox = person.to_tlbr()
        pmx, pmy, pMx, pMy = int(pbox[0]), int(pbox[1]), int(pbox[2]), int(pbox[3])
        cnt = False

        #스쿠터 목록중에서, 사람이 탑승 중인 것이 있는 지확인
        for track in scooter_list:
            bbox = track.to_tlbr()
            smx, smy, sMx, sMy = int(bbox[0]), int(bbox[1]), int(bbox[2]), int(bbox[3])
            # 중점 킥보드 사이에 있는가
            if smx < (pmx + pmx) / 2 < sMx:
                # 사람의 객체의 잇점이 전동킥보드 보다 위에 있고, 밑점이 전동킥보드의 윗점 아래있는가
                # if 2*smy -sMy < pmy and pmy <smy :
                if pmy < smy:
                    # 사람 객체의 밑점이 전동킥보드 밑점보다 위인가
                    if smy < pMy < sMy:
                        return True, track

        return False, None


    # 감지된 객체를 opencv를 통해서 화면상에 그려준다.
    def draw_box(self, track, frame, colors, bbox, class_name):
        # draw bbox on screen
        color = colors[int(track.track_id) % len(colors)]
        color = [i * 255 for i in color]
        cv2.rectangle(frame, (int(bbox[0]), int(bbox[1])), (int(bbox[2]), int(bbox[3])), color, 2)
        cv2.rectangle(frame, (int(bbox[0]), int(bbox[1] - 30)),
                      (int(bbox[0]) + (len(class_name) + len(str(track.track_id))) * 17, int(bbox[1])), color, -1)
        cv2.putText(frame, class_name + "-" + str(track.track_id), (int(bbox[0]), int(bbox[1] - 10)), 0, 0.75,
                    (255, 255, 255), 2)
        # 좌표를 화면상에 표시
        # position = str(int(bbox[0])) +" "+  str(int(bbox[1])) +" "+  str(int(bbox[2])) +" "+  str(int(bbox[3]))
        # cv2.putText(frame, position, (int(bbox[0]), int(bbox[1] - 20)), 0, 0.75,
        #            (255, 255, 255), 2)

    # 감지된 전동스쿠터와 사람 객체를 분석해서,
    def detect_rider(self, class_name, scooter_list, scooter_time, track, riders, rider_time, now_time, riding_state,
                     not_ridecount, rider_list):
        if class_name == "electric scooter":
            scooter_list.append(track)
            if not scooter_time[track.track_id]:
                # scooter_time은 기본형이 리스트인 딕셔너리이다. 전동스쿠터 객체의 아이디를 키로 사용한다.
                # 처음 발견된 객체이면, 지금 시간을 0번 배열값, 1번 배열 값으로 -1을넣는다
                # 후에 한번더 관측되면, 1번 배열의 값을 관측된 시간으로 하여서, 마지막으로 등장한 시간을 계속 기록한다.
                scooter_time[track.track_id].append(now_time)
                scooter_time[track.track_id].append(-1)

            # TODO: 실시간 영상에 적용될 경우, 오래된 킥보드들을 scooter_list에서 제거하는 코드 필요
            # 공유 킥보드를 탑승자가 반납한 후, 한참 뒤에 새로운 사람이 대여하는 경우
            # 인식된 킥보드가 탑승자가 있는 킥보드이고
            if track in list(riders.keys()):
                olders = []
                for old_rider in riders[track]:
                    # 킥보드 탑승자와 킥보드의 마지막 등장시간이 20초 이상 차이날 경우
                    if abs(rider_time[old_rider.track_id][1] - scooter_time[track.track_id][1]) > 20:
                        olders.append(old_rider)
                        riders[track].remove(old_rider)
                # 탑승자가 2명 이상일 경우
                if len(olders) >= 2:
                    old_rider.append(track.track_id, olders, scooter_time[track.track_id])
                    # print("2명 탑승!", old_rider)

        if class_name == "person":
            cnt, scooter = self.isRide(scooter_list, track)
            if cnt:
                # 이미 등록된 스쿠터이면, 마지막으로 등장한 시간을 업데이트한다.
                scooter_time[scooter.track_id][1] = now_time
                not_ridecount[track] = 0
                if not rider_time[track.track_id]:
                    rider_time[track.track_id].append(now_time)
                    rider_time[track.track_id].append(-1)
                else:
                    rider_time[track.track_id][1] = now_time

                # 처음으로 탑승한 것으로 판단된 사람이면
                if not riding_state[track]:
                    riding_state[track] = True
                    if riders[scooter].count(track) == 0:
                        rider_list.append(track)
                        riders[scooter].append(track)
            else:
                # 탑승자가 아닌 사람이, 라이더 리스트에 있을 때
                if track in rider_list:
                    # 탑승 시간이 2초 이상되면, 정차 후 내린 것으로 판단해서, 라이더 목록에서 제거하지 않는다.
                    # if rider_time[track.track_id][1] - rider_time[track.track_id][0] <=2.0:
                    if not not_ridecount[track]:
                        not_ridecount[track] = 1
                    else:
                        not_ridecount[track] += 1
                    print(track.track_id, "탑승자 아님", not_ridecount[track])
                    for sc in riders:
                        # 탑승자가 탑승중인 스쿠터를 찾고
                        if track in riders[sc]:
                            #등장시간이 2초 이상 된 경우, 탑승 시간이 2초가 안되거나 70번이상 ㄷ
                            if now_time - rider_time[track.track_id][0] > 2:
                                if abs(rider_time[track.track_id][1] - rider_time[track.track_id][0]) <= 2 or \
                                        not_ridecount[track] >= 70:
                                    # 제거
                                    print("라이더" + str(track.track_id) + "스쿠터 " + str(sc.track_id) + "에서 OUT",
                                          riders[sc],
                                          sc.age, track.age, scooter_time[sc.track_id], now_time)
                                    # if track in riders[sc]:
                                    # rider_time[track.track_id][0] = now_time
                                    riders[sc].remove(track)
                                    not_ridecount[track] -= 35
                                    if riding_state[track]:
                                        riding_state[track] = False
                            else:
                                if not_ridecount[track] >= 40:
                                    # 제거
                                    print("라이더" + str(track.track_id) + "스쿠터 " + str(sc.track_id) + "에서 OUT",
                                          riders[sc],
                                          sc.age, track.age, scooter_time[sc.track_id], now_time)
                                    # if track in riders[sc]:
                                    # rider_time[track.track_id][0] = now_time
                                    riders[sc].remove(track)
                                    not_ridecount[track] -= 30
                                    if riding_state[track]:
                                        riding_state[track] = False

            for ri in rider_list:
                if track.track_id == ri.track_id:
                    rider_time[track.track_id][1] = now_time

    def detect(self):
        while True:
            return_value, frame = self.vid.read()
            #영상이 돌아가서 출력될 경우, --Rotate Ture 옵션을 주면 영상을 90도 회전 시켜준다.
            if FLAGS.rotate:
                frame = cv2.transpose(frame)
                frame = cv2.flip(frame, 1)
            #영상 사에서 현재 프레임의 시간
            now_time = self.vid.get(cv2.CAP_PROP_POS_MSEC) / 1000
            if return_value:
                frame = cv2.cvtColor(frame, cv2.COLOR_BGR2RGB)
                image = Image.fromarray(frame)
            else:
                print('Video has ended or failed, try a different video format!')
                break
            self.frame_num += 1
            print('Frame #: ', self.frame_num)
            frame_size = frame.shape[:2]
            # YOLO 모델에서 사용된 input size와 똑같이 맞춰준다.
            image_data = cv2.resize(frame, (self.input_size, self.input_size))
            # 픽셀 값들을 0과 1사이로 정규화
            image_data = image_data / 255.
            image_data = image_data[np.newaxis, ...].astype(np.float32)
            start_time = time.time()

            # Detecting을 위해 이미지를 탠서 객체로 변환
            batch_data = tf.constant(image_data)
            pred_bbox = self.infer(batch_data)
            for key, value in pred_bbox.items():
                boxes = value[:, :, 0:4]
                pred_conf = value[:, :, 4:]

            boxes, scores, classes, valid_detections = tf.image.combined_non_max_suppression(
                boxes=tf.reshape(boxes, (tf.shape(boxes)[0], -1, 1, 4)),
                scores=tf.reshape(
                    pred_conf, (tf.shape(pred_conf)[0], -1, tf.shape(pred_conf)[-1])),
                max_output_size_per_class=50,
                max_total_size=50,
                iou_threshold=FLAGS.iou,
                score_threshold=FLAGS.score
            )

            # convert data to numpy arrays and slice out unused elements
            num_objects = valid_detections.numpy()[0]
            bboxes = boxes.numpy()[0]
            bboxes = bboxes[0:int(num_objects)]
            scores = scores.numpy()[0]
            scores = scores[0:int(num_objects)]
            classes = classes.numpy()[0]
            classes = classes[0:int(num_objects)]

            # format bounding boxes from normalized ymin, xmin, ymax, xmax ---> xmin, ymin, width, height
            original_h, original_w, _ = frame.shape
            bboxes = utils.format_boxes(bboxes, original_h, original_w)

            # store all predictions in one parameter for simplicity when calling functions
            pred_bbox = [bboxes, scores, classes, num_objects]

            # read in all class names from config
            class_names = utils.read_class_names(cfg.YOLO.CLASSES)

            # by default allow all classes in .names file
            allowed_classes = list(class_names.values())

            # loop through objects and use class index to get class name, allow only classes in allowed_classes list
            names = []
            deleted_indx = []
            for i in range(num_objects):
                # print(i)
                class_indx = int(classes[i])
                class_name = class_names[class_indx]
                if class_name not in allowed_classes:
                    deleted_indx.append(i)
                else:
                    names.append(class_name)
            names = np.array(names)
            count = len(names)
            if FLAGS.count:
                cv2.putText(frame, "Objects being tracked: {}".format(count), (5, 35), cv2.FONT_HERSHEY_COMPLEX_SMALL,
                            2, (0, 255, 0), 2)
                print("Objects being tracked: {}".format(count))
            # delete detections that are not in allowed_classes
            bboxes = np.delete(bboxes, deleted_indx, axis=0)
            scores = np.delete(scores, deleted_indx, axis=0)

            # encode yolo detections and feed to tracker
            features = self.encoder(frame, bboxes)
            detections = [Detection(bbox, score, class_name, feature) for bbox, score, class_name, feature in
                          zip(bboxes, scores, names, features)]

            # initialize color map
            cmap = plt.get_cmap('tab20b')
            colors = [cmap(i)[:3] for i in np.linspace(0, 1, 20)]

            # run non-maxima supression
            boxs = np.array([d.tlwh for d in detections])
            scores = np.array([d.confidence for d in detections])
            classes = np.array([d.class_name for d in detections])
            indices = preprocessing.non_max_suppression(boxs, classes, self.nms_max_overlap, scores)
            detections = [detections[i] for i in indices]

            # Call the tracker
            self.tracker.predict()
            self.tracker.update(detections)

            # update tracks
            for track in self.tracker.tracks:
                if not track.is_confirmed() or track.time_since_update > 1:
                    continue

                # 현재 박스 포맷을 리턴함(min x, miny, max x, max y) numpy array값으로
                bbox = track.to_tlbr()
                class_name = track.get_class()

                self.draw_box(track, frame, colors, bbox, class_name)

                self.detect_rider(class_name, self.scooter_list, self.scooter_time, track, self.riders, self.rider_time, now_time, self.riding_state,
                             self.not_ridecount, self.rider_list)

                # if enable info flag then print details about each track
                if FLAGS.info:
                    print("Tracker ID: {}, Class: {},  BBox Coords (xmin, ymin, xmax, ymax): {}".format(
                        str(track.track_id), class_name, (int(bbox[0]), int(bbox[1]), int(bbox[2]), int(bbox[3]))))

            # calculate frames per second of running detections
            fps = 1.0 / (time.time() - start_time)
            print("FPS: %.2f" % fps)
            result = np.asarray(frame)
            result = cv2.cvtColor(frame, cv2.COLOR_RGB2BGR)

            if not FLAGS.dont_show:
                cv2.imshow("Output Video", result)


            for r in self.riders:
                if len(self.riders[r]) >= 2:
                    if FLAGS.save_image:
                        if not self.save_cnt[r]:
                            # 만든시간을 타임 스탬프로 출력
                            ctime = os.path.getctime(self.video_path)
                            file_name = str(ctime+now_time)
                            print(file_name)
                            cv2.imwrite(f'./outputs/images/{file_name}.png', result)
                            self.save_cnt[r] = True
                    ids = []
                    for p in self.riders[r]:
                        ids.append(p.track_id)
                    print("위법 행위 감지!! (2명 이상 탑승)!!", "스쿠터 ID: ", r.track_id, "탑승자 ID: ", ids)

            # if output flag is set, save video file
            if FLAGS.output:
                self.out.write(result)
            if cv2.waitKey(1) & 0xFF == ord('q'): break
        cv2.destroyAllWindows()

        #감지된 탑승자들을 전부 출력
        for r in self.riders:
            if len(self.riders[r]) >= 2:
                ids = []
                for p in self.riders[r]:
                    ids.append(p.track_id)
                print("2명 이상 탑승!!", "스쿠터 ID: ", r.track_id, "탑승자 ID: ", ids)

        if self.old_riders:
            print("2명 이상 탑승!!", self.old_riders)
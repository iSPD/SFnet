/*------------------------------------------------------------------------------------------*\
This file contains material supporting chapter 5 of the cookbook:
Computer Vision Programming using the OpenCV Library.
by Robert Laganiere, Packt Publishing, 2011.

This program is free software; permission is hereby granted to use, copy, modify,
and distribute this source code, or portions thereof, for any purpose, without fee,
subject to the restriction that the copyright notice may not be removed
or altered from any source or altered source distribution.
The software is released on an as-is basis and without any warranties of any kind.
In particular, the software is not guaranteed to be fault-tolerant or free from failure.
The author disclaims all warranties with regard to this software, any use,
and any consequent failure, is purely the responsibility of the user.

Copyright (C) 2010-2011 Robert Laganiere, www.laganiere.name
\*------------------------------------------------------------------------------------------*/

#if !defined WATERSHS
#define WATERSHS

#include <opencv2/core/core.hpp>
#include <opencv2/imgproc/imgproc.hpp>

class WatershedSegmenter {

private:

	cv::Mat markers;

public:

	void setMarkers(const cv::Mat& markerImage) {
		markerImage.convertTo(markers, CV_32SC1);
	}

	cv::Mat process(const cv::Mat &image) {

		cv::watershed(image, markers);

		return markers;
	}

	cv::Mat getSegmentation() {

		cv::Mat tmp;
		markers.convertTo(tmp, CV_8U);		

		return tmp;
	}

	cv::Mat getWatersheds() {

		cv::Mat tmp;
		markers.convertTo(tmp, CV_8U, 255, 255);

		return tmp;
	}
};



#endif
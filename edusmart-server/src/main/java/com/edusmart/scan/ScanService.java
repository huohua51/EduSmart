package com.edusmart.scan;

import com.edusmart.scan.dto.ScanRequest;
import com.edusmart.security.AuthenticatedUser;
import com.edusmart.wrongquestion.WrongQuestionService;
import com.edusmart.wrongquestion.dto.WrongQuestionRequest;
import com.edusmart.wrongquestion.dto.WrongQuestionResponse;
import org.springframework.stereotype.Service;

@Service
public class ScanService {

    private final WrongQuestionService wrongQuestionService;

    public ScanService(WrongQuestionService wrongQuestionService) {
        this.wrongQuestionService = wrongQuestionService;
    }

    /**
     * 保存拍照识题结果为错题
     */
    public WrongQuestionResponse saveAsWrongQuestion(AuthenticatedUser user, ScanRequest req) {
        WrongQuestionRequest wqReq = new WrongQuestionRequest();
        wqReq.setQuestionText(req.getQuestionText());
        wqReq.setAnswer(req.getAnswer());
        wqReq.setAnalysis(req.getAnalysis());
        wqReq.setSteps(req.getSteps());
        wqReq.setKnowledgePoints(req.getKnowledgePoints());
        wqReq.setReviewCount(0);
        wqReq.setNextReviewTime(System.currentTimeMillis() + 24L * 60 * 60 * 1000);
        wqReq.setCreatedAt(System.currentTimeMillis());
        return wrongQuestionService.create(user, wqReq);
    }
}

package com.digitalpetri.enip.cip.services;

import com.digitalpetri.enip.cip.CipResponseException;
import com.digitalpetri.enip.cip.epath.EPath.PaddedEPath;
import com.digitalpetri.enip.cip.epath.LogicalSegment.ClassId;
import com.digitalpetri.enip.cip.epath.LogicalSegment.InstanceId;
import com.digitalpetri.enip.cip.structs.ForwardOpenRequest;
import com.digitalpetri.enip.cip.structs.ForwardOpenResponse;
import com.digitalpetri.enip.cip.structs.MessageRouterRequest;
import com.digitalpetri.enip.cip.structs.MessageRouterResponse;
import io.netty.buffer.ByteBuf;
import io.netty.util.ReferenceCountUtil;

public class ForwardOpenService implements CipService<ForwardOpenResponse> {

    public static final int SERVICE_CODE = 0x54;

    private static final PaddedEPath CONNECTION_MANAGER_PATH = new PaddedEPath(
            new ClassId(0x06),
            new InstanceId(0x01)
    );

    private final ForwardOpenRequest request;

    public ForwardOpenService(ForwardOpenRequest request) {
        this.request = request;
    }

    @Override
    public void encodeRequest(ByteBuf buffer) {
        MessageRouterRequest mrr = new MessageRouterRequest(
                SERVICE_CODE,
                CONNECTION_MANAGER_PATH,
                this::encode
        );

        MessageRouterRequest.encode(mrr, buffer);
    }

    @Override
    public ForwardOpenResponse decodeResponse(ByteBuf buffer) throws CipResponseException, PartialResponseException {
        MessageRouterResponse mResponse = MessageRouterResponse.decode(buffer);

        int generalStatus = mResponse.getGeneralStatus();

        try {
            if (generalStatus == 0x00) {
                return ForwardOpenResponse.decode(mResponse.getData());
            } else {
                throw new CipResponseException(generalStatus, mResponse.getAdditionalStatus());
            }
        } finally {
            ReferenceCountUtil.release(mResponse.getData());
        }
    }

    private void encode(ByteBuf buffer) {
        ForwardOpenRequest.encode(request, buffer);
    }

}

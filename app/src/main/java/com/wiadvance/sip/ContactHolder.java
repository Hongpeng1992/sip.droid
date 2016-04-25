package com.wiadvance.sip;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.daimajia.swipe.SwipeLayout;
import com.squareup.picasso.Picasso;
import com.wiadvance.sip.db.ContactDbHelper;
import com.wiadvance.sip.model.CallLogEntry;
import com.wiadvance.sip.model.Contact;

public class ContactHolder extends RecyclerView.ViewHolder {

    private static final String TAG = "ContactHolder";

    private final View mRootItemView;
    private final TextView mNameTextView;
    private final ImageView mPhoneImageview;
    private final ImageView mAvatar;
    private final Context mContext;
    private final SwipeLayout mRootItemSwipeLayoutView;
    private final ImageView mFavoriteImageView;
    private final ImageView mNotFavoriteImageView;
    private boolean isButtonDisplayed;
    private final View mBottomWrapperView;

    private boolean isSwiping = false;
    private final FrameLayout mFavorite_frame_layout;

    public ContactHolder(Context context, View itemView) {
        super(itemView);

        mContext = context;
        mRootItemView = itemView;

        mRootItemSwipeLayoutView = (SwipeLayout) itemView.findViewById(R.id.swipe_layout);
        mNameTextView = (TextView) itemView.findViewById(R.id.contact_name_text_view);
        mPhoneImageview = (ImageView) itemView.findViewById(R.id.phone_icon_image_view);
        mAvatar = (ImageView) itemView.findViewById(R.id.list_item_avatar);
        mBottomWrapperView = itemView.findViewById(R.id.bottom_wrapper);

        mFavoriteImageView = (ImageView) itemView.findViewById(R.id.is_favorite_image_view);
        mNotFavoriteImageView = (ImageView) itemView.findViewById(R.id.not_favorite_image_view);
        mFavorite_frame_layout = (FrameLayout) itemView.findViewById(R.id.favorite_frame_layout);
    }

    public void bindContactViewHolder(final Contact contact) {
        bindContact(contact);
    }

    public void bindCallLogContactViewHolder(CallLogEntry log) {
        if (log.getContact() != null) {
            bindContact(log.getContact());
        }

        ImageView callStatusImageView = (ImageView) mRootItemView.findViewById
                (R.id.call_status_image_view);
        TextView callMsgTextView = (TextView) mRootItemView.findViewById(R.id.call_msg);

        TextView callTimeTextView = (TextView) mRootItemView.findViewById(R.id.call_time);
        String callTime = String.format(mContext.getResources().getString(R.string.call_time), log.getCallTimeString());
        callTimeTextView.setText(callTime);

        String callDurationTime = mContext.getResources().getQuantityString(R.plurals.call_duration_time, log.getCallDurationInSeconds(), log.getCallDurationInSeconds());

        if (log.getCallType() == CallLogEntry.TYPE_INCOMING_CALL_ANSWERED) {
            callStatusImageView.setImageResource(R.drawable.call_received_blue_16dp);
            callMsgTextView.setText(log.getCallDurationInSeconds());
            callMsgTextView.setText(callDurationTime);

        } else if (log.getCallType() == CallLogEntry.TYPE_INCOMING_CALL_NO_ANSWER) {
            callStatusImageView.setImageResource(R.drawable.call_missed_red_16dp);

        } else if (log.getCallType() == CallLogEntry.TYPE_OUTGOING_CALL_ANSWERED) {
            callStatusImageView.setImageResource(R.drawable.call_made_green_16dp);
            callMsgTextView.setText(callDurationTime);

        } else {
            // outgoing call not answered
            callMsgTextView.setText(R.string.no_answer_msg);
            callStatusImageView.setImageResource(R.drawable.call_made_green_16dp);
        }
    }

    private void bindContact(final Contact contact) {
        mNameTextView.setText(contact.getId() + "/" + contact.getName() + contact.getType());

        int scale = Utils.getDeviceScale(mContext);
        if (!UserData.sAvatar404Cache.contains(contact.getPhotoUri())) {
            Picasso.with(mContext).load(contact.getPhotoUri()).resize(40 * scale, 40 * scale)
                    .placeholder(R.drawable.avatar_120dp).into(mAvatar);
        } else {
            mAvatar.setImageResource(R.drawable.avatar_120dp);
        }

        mPhoneImageview.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                PhoneUtils.call(mContext, contact);
            }
        });

        mRootItemSwipeLayoutView.setLeftSwipeEnabled(false);
        mRootItemSwipeLayoutView.setRightSwipeEnabled(false);
        mRootItemSwipeLayoutView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                if (!isSwiping) {
                    PhoneUtils.call(mContext, contact);
                }
                return true;
            }
        });

        mRootItemSwipeLayoutView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleButtonsRelativeLayout();
            }
        });

        mBottomWrapperView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleButtonsRelativeLayout();
            }
        });

        if (contact.isFavorite(mContext)) {
            showFavorite(true);
        } else {
            showFavorite(false);
        }

        mFavorite_frame_layout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (contact.isFavorite(mContext)) {
                    showFavorite(false);
                    ContactDbHelper.getInstance(mContext).removeFavoriteContact(contact);
                } else {
                    showFavorite(true);
                    ContactDbHelper.getInstance(mContext).addFavoriteContact(contact);
                }
            }
        });
    }

    private void showFavorite(boolean show) {
        if (show) {
            mNameTextView.setTextColor(mContext.getResources().getColor(R.color.red));
            mFavoriteImageView.setVisibility(View.VISIBLE);
            mNotFavoriteImageView.setVisibility(View.GONE);
        } else {
            mNameTextView.setTextColor(mContext.getResources().getColor(R.color.dark_gray));
            mFavoriteImageView.setVisibility(View.GONE);
            mNotFavoriteImageView.setVisibility(View.VISIBLE);
        }
    }

    private void toggleButtonsRelativeLayout() {
        if (isButtonDisplayed) {
            mRootItemSwipeLayoutView.close(true);
            isButtonDisplayed = false;
        } else {
            mBottomWrapperView.setVisibility(View.VISIBLE);
            mRootItemSwipeLayoutView.open(true);
            isButtonDisplayed = true;
        }
    }
}
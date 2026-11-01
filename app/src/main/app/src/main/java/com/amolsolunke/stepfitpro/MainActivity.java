private final BroadcastReceiver stepsReceiver = new BroadcastReceiver() {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (content != null) {
            showPage(page);
        }
    }
};

private boolean receiverRegistered = false;

@Override
protected void onStart() {
    super.onStart();

    if (!receiverRegistered) {
        IntentFilter filter = new IntentFilter(
            "com.amolsolunke.stepfitpro.STEPS_UPDATED"
        );

        if (Build.VERSION.SDK_INT >= 33) {
            registerReceiver(stepsReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(stepsReceiver, filter);
        }

        receiverRegistered = true;
    }
}

@Override
protected void onStop() {
    if (receiverRegistered) {
        unregisterReceiver(stepsReceiver);
        receiverRegistered = false;
    }

    super.onStop();
}

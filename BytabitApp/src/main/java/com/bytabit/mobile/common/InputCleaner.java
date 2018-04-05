package com.bytabit.mobile.common;

import io.reactivex.ObservableTransformer;
import io.reactivex.rxjavafx.sources.Change;
import io.reactivex.schedulers.Schedulers;


public class InputCleaner {

    public static ObservableTransformer<Change<String>, String> numbers() {

        final String regex = "(^$)|(^[0-9]{1,5}[.]?)|(^[0-9]{1,5}[.][0-9]{0,6})";

        return changes -> changes.observeOn(Schedulers.io()).map(change ->
                change.getNewVal().matches(regex) ? change.getNewVal() : change.getOldVal());
    }

//    public static ObservableTransformer<Change<String>, Change<String>> numbers() {
//
//        final String regex = "(^[0-9]{1,4}[.]?)|(^[0-9]{1,4}[.][0-9]{0,6})";
//
//        return changes -> changes.observeOn(Schedulers.io()).map(change -> {
//            final String oldVal = change.getOldVal();
//            final String newVal = change.getNewVal();
//
//            if (newVal == null || newVal.isEmpty()) {
//                return new Change<>(oldVal, null);
//            } else if (!newVal.matches(regex)) {
//                if (oldVal.matches(regex)) {
//                    return new Change<>(newVal, oldVal);
//                } else {
//                    return new Change<>(newVal, null);
//                }
//            } else {
//                return new Change<>(newVal, newVal);
//            }
//        }).filter(c -> !c.getOldVal().equals(c.getNewVal()));
//    }
}

package com.yury.trade.repository;

import com.yury.trade.entity.Option;
import org.springframework.data.repository.CrudRepository;

public interface OptionRepository  extends CrudRepository<Option, String> {
}

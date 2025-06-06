/**
 * Central domain abstractions especially to be used in combination with the
 * {@link org.springframework.data.repository.Repository} abstraction.
 *
 * @see org.springframework.data.repository.Repository
 */
@XmlSchema(
		xmlns = { @XmlNs(prefix = "sd", namespaceURI = org.springframework.data.domain.jaxb.SpringDataJaxb.NAMESPACE) })
@XmlJavaTypeAdapters({
		@XmlJavaTypeAdapter(value = org.springframework.data.domain.jaxb.PageableAdapter.class, type = Pageable.class),
		@XmlJavaTypeAdapter(value = org.springframework.data.domain.jaxb.SortAdapter.class, type = Sort.class),
		@XmlJavaTypeAdapter(value = org.springframework.data.domain.jaxb.PageAdapter.class, type = Page.class) })
@org.jspecify.annotations.NullMarked
package org.springframework.data.domain.jaxb;

import jakarta.xml.bind.annotation.XmlNs;
import jakarta.xml.bind.annotation.XmlSchema;
import jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapters;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
